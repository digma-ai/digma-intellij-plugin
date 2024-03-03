package org.digma.intellij.plugin.errorreporting

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.UntraceableException
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.NoSelectedEnvironmentException
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.common.UserId
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.semanticversion.SemanticVersionUtil
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.Field
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

const val SEVERITY_PROP_NAME = "severity"
const val SEVERITY_LOW_NO_FIX = "low, reporting only,no need to fix"
const val SEVERITY_MEDIUM_TRY_FIX = "medium, try to fix"
const val SEVERITY_HIGH_TRY_FIX = "high, try to fix"

@Service(Service.Level.APP)
class ErrorReporter {

    private val logger: Logger = Logger.getInstance(this::class.java)


    companion object {

        @JvmStatic
        fun getInstance(): ErrorReporter {
            return service<ErrorReporter>()
        }
    }


    /*
        try not to use this method and always send the project reference if available.
     */
    fun reportError(message: String, t: Throwable) {
        reportError(findActiveProject(), message, t)
    }

    /*
        try not to use this method and always send the project reference if available.
     */
    fun reportError(message: String, t: Throwable, extraDetails: Map<String, String>) {
        reportError(findActiveProject(), message, t, extraDetails)
    }

    /*
        message is actually more a hint of where the error happened for quickly understanding that from the error event.
        see usage examples.
        the event will contain the stack trace and exception message.
     */
    fun reportError(project: Project?, message: String, throwable: Throwable) {
        reportError(
            project, message, throwable, mapOf(
                SEVERITY_PROP_NAME to SEVERITY_HIGH_TRY_FIX
            )
        )
    }

    //this method is used to report an error that is not an exception. it should contain some details to say what the error is
    fun reportError(project: Project?, message: String, action: String, details: Map<String, String>) {


        if (isTooFrequentError(message, action)) {
            return
        }


        val projectToUse = project ?: findActiveProject()

        projectToUse?.let {
            if (it.isDisposed) return

            val detailsToSend = details.toMutableMap()
            detailsToSend["action"] = action
            if (!detailsToSend.containsKey(SEVERITY_PROP_NAME)) {
                detailsToSend[SEVERITY_PROP_NAME] = SEVERITY_HIGH_TRY_FIX
            }

            ActivityMonitor.getInstance(it).registerError(null, message, detailsToSend)
        }
    }


    fun reportError(project: Project?, message: String, throwable: Throwable, details: Map<String, String>) {

        try {
            //many times the exception is no-connection exception, and that may happen too many times.
            // when there is no connection all timers will get an AnalyticsService exception every 10 seconds, it's useless
            //to report that. AnalyticsService exceptions are reported separately and will include no-connection exceptions.
            if (ExceptionUtils.isConnectionException(throwable) || throwable is NoSelectedEnvironmentException) {
                return
            }

            if (isTooFrequentException(message, throwable)) {
                return
            }

            //todo: change ActivityMonitor to application service so no need for project

            val projectToUse = project ?: findActiveProject()

            projectToUse?.let {
                if (it.isDisposed) return

                //add SEVERITY_HIGH_TRY_FIX if severity doesn't exist
                val detailsToSend = if (details.containsKey(SEVERITY_PROP_NAME)) {
                    details
                } else {
                    val mm = details.toMutableMap()
                    mm[SEVERITY_PROP_NAME] = SEVERITY_HIGH_TRY_FIX
                    mm
                }
                ActivityMonitor.getInstance(it).registerError(throwable, message, detailsToSend)
            }
        } catch (e: Exception) {
            Log.warnWithException(logger, e, "error in error reporter")
        }
    }


    /*
       message is actually more a hint of where the error happened for quickly understanding that from the error event.
       see usage examples.
       the event will contain the stack trace and exception message.
     */
    fun reportAnalyticsServiceError(project: Project?, message: String, methodName: String, exception: Exception, isConnectionException: Boolean) {

        try {
            if (isTooFrequentException(message, exception)) {
                return
            }

            //todo: change ActivityMonitor to application service so no need for project

            val projectToUse = project ?: findActiveProject()

            projectToUse?.let {
                if (it.isDisposed) return
                ActivityMonitor.getInstance(it).registerAnalyticsServiceError(exception, message, methodName, isConnectionException)
            }
        } catch (e: Exception) {
            Log.warnWithException(logger, e, "error in error reporter")
        }
    }


    fun reportBackendError(message: String, action: String) {
        reportBackendError(findActiveProject(), message, action)
    }

    fun reportBackendError(project: Project?, message: String, action: String) {
        if (isTooFrequentBackendError(message, action)) {
            return
        }

        val projectToUse = project ?: findActiveProject()

        projectToUse?.let {
            ActivityMonitor.getInstance(it).reportBackendError(message, action)
        }
    }


    //this error should be reported only when its a fatal error that we must fix quickly.
    //don't use it for all errors.
    //currently will be reported from EDT.assertNonDispatchThread and ReadActions.assertNotInReadAccess
    // which usually should be caught in development but if not, are very urgent to fix.
    fun reportInternalFatalError(source: String, exception: Throwable?) {

        val details = mutableMapOf<String, String>()
        details["Note"] = "This is an internal reporting of urgent error caught by plugin code and should be fixed ASAP"

        //todo: change ActivityMonitor to application service
        val projectToUse = findActiveProject() ?: return

        exception?.let {
            val exceptionStackTrace = it.let {
                val stringWriter = StringWriter()
                exception.printStackTrace(PrintWriter(stringWriter))
                stringWriter.toString()
            }

            val exceptionMessage: String = it.let {
                ExceptionUtils.getNonEmptyMessage(it)
            } ?: ""

            val causeExceptionType = ExceptionUtils.getFirstRealExceptionCauseTypeName(it)

            details["exception.message"] = exceptionMessage
            details["exception.stack-trace"] = exceptionStackTrace
            details["cause.exception.type"] = causeExceptionType
            details["exception.type"] = it.javaClass.name
        }

        val osType = System.getProperty("os.name")
        val ideInfo = ApplicationInfo.getInstance()
        val ideName = ideInfo.versionName
        val ideVersion = ideInfo.fullVersion
        val ideBuildNumber = ideInfo.build.asString()
        val pluginVersion = SemanticVersionUtil.getPluginVersionWithoutBuildNumberAndPreRelease("unknown")

        details["ide.name"] = ideName
        details["ide.version"] = ideVersion
        details["ide.build"] = ideBuildNumber
        details["plugin.version"] = pluginVersion
        details["os.type"] = osType
        details["user.type"] = if (UserId.isDevUser) "internal" else "external"
        details["error source"] = source

        ActivityMonitor.getInstance(projectToUse).registerFatalError(details)

    }



    private fun isTooFrequentBackendError(message: String, action: String): Boolean {
        val counter = MyCache.getOrCreate(message, action)
        val occurrences = counter.incrementAndGet()
        return occurrences > 1
    }

    private fun isTooFrequentError(message: String, action: String): Boolean {
        val counter = MyCache.getOrCreate(message, action)
        val occurrences = counter.incrementAndGet()
        return occurrences > 1
    }


    private fun isTooFrequentException(message: String, t: Throwable): Boolean {
        val hash = computeAccurateTraceHashCode(t)
        val counter = MyCache.getOrCreate(hash, t, message)
        val occurrences = counter.incrementAndGet()
        return occurrences > 1
    }


    private fun computeAccurateTraceHashCode(throwable: Throwable): Int {
        val backtrace = getBacktrace(throwable)
        if (backtrace == null) {
            val trace = if (throwable is UntraceableException) null else throwable.stackTrace
            return trace.contentHashCode()
        }
        return backtrace.contentDeepHashCode()
    }


    private fun getBacktrace(throwable: Throwable): Array<Any>? {

        val backtrace = try {

            val backtraceField: Field? = com.intellij.util.ReflectionUtil.getDeclaredField(Throwable::class.java, "backtrace")
            if (backtraceField != null) {
                backtraceField.isAccessible = true
                backtraceField.get(throwable)
            } else {
                null
            }

        } catch (e: Throwable) {
            null
        }

        if (backtrace != null && backtrace is Array<*> && backtrace.isArrayOf<Any>()) {
            @Suppress("UNCHECKED_CAST")
            return backtrace as Array<Any>
        }

        return null

    }

}


private object MyCache {
    private val cache = Caffeine.newBuilder()
        .maximumSize(1000)
        //expireAfterAccess means we don't send the error as long as it keeps occurring until it is quite for this time,
        //if it reappears send it again
//        .expireAfterAccess(10, TimeUnit.MINUTES)
        //expireAfterWrite mean the error will be sent in fixed intervals.
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build<String, AtomicInteger>()

    fun getOrCreate(hash: Int, t: Throwable, message: String): AtomicInteger {
        return cache.get("$hash:$t:$message") { AtomicInteger() }
    }

    fun getOrCreate(message: String, action: String): AtomicInteger {
        return cache.get("$message:$action") { AtomicInteger() }
    }
}

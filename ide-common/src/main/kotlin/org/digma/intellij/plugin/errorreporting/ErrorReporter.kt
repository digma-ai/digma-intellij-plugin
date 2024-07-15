package org.digma.intellij.plugin.errorreporting

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bind.annotation.RuntimeType
import net.bytebuddy.matcher.ElementMatchers
import org.digma.intellij.plugin.analytics.NoSelectedEnvironmentException
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.common.FrequencyDetector
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration


const val SEVERITY_PROP_NAME = "severity"
const val SEVERITY_LOW_NO_FIX = "low, reporting only,no need to fix"
const val SEVERITY_MEDIUM_TRY_FIX = "medium, try to fix"
const val SEVERITY_HIGH_TRY_FIX = "high, try to fix"

//class and all public methods should be open so that NoOpProxy will work
open class ErrorReporter {

    private val logger: Logger = Logger.getInstance(this::class.java)

    private val frequencyDetector = FrequencyDetector(60.minutes.toJavaDuration())

    //must be public class
    class MyPauseInterceptor {

        companion object {
            @JvmStatic
            @RuntimeType
            @Throws(Throwable::class)
            fun intercept(): Any? {
                return null
            }
        }
    }

    companion object {

        private var active = true

        //a proxy that is returned from getInstance when ErrorReporter is paused.
        // the proxy will be returned only when using ErrorReporter.getInstance() which should be the convention
        // for getting a reference to a service. but one can call service<ErrorReported> which will return the real one
        private fun getNoOpProxy() = try {
            ByteBuddy()
                .subclass(ErrorReporter::class.java)
                .method(ElementMatchers.isDeclaredBy<MethodDescription?>(ErrorReporter::class.java).and(ElementMatchers.isPublic()))
                .intercept(
                    MethodDelegation.to(MyPauseInterceptor::class.java)
                )
                .make()
                .load(ErrorReporter::class.java.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .loaded.getConstructor().newInstance()

        } catch (e: Throwable) {
            Log.warnWithException(Logger.getInstance(this::class.java), e, "can not create bytebuddy proxy")
            service<ErrorReporter>().reportError("ErrorReporter.getNoOpProxy", e)
            service<ErrorReporter>()
        }


        @JvmStatic
        fun getInstance(): ErrorReporter {
            return if (active) {
                service<ErrorReporter>()
            } else {
                getNoOpProxy()
            }
        }

        fun pause() {
            active = false
        }

        fun resume() {
            active = true
        }
    }


    /*
        try not to use this method and always send the project reference if available.
     */
    open fun reportError(message: String, t: Throwable) {
        reportError(findActiveProject(), message, t)
    }

    /*
        try not to use this method and always send the project reference if available.
     */
    open fun reportError(message: String, t: Throwable, extraDetails: Map<String, String>) {
        reportError(findActiveProject(), message, t, extraDetails)
    }

    /*
        message is actually more a hint of where the error happened for quickly understanding that from the error event.
        see usage examples.
        the event will contain the stack trace and exception message.
     */
    open fun reportError(project: Project?, message: String, throwable: Throwable) {
        reportError(
            project, message, throwable, mapOf(
                SEVERITY_PROP_NAME to SEVERITY_HIGH_TRY_FIX
            )
        )
    }

    private fun isTooFrequent(message: String, stackTrace: String?): Boolean {
        if (!stackTrace.isNullOrEmpty()) {
            return frequencyDetector.isTooFrequentStackTrace(message, stackTrace)
        }
        return frequencyDetector.isTooFrequentError(message, "")
    }

    open fun reportError(message: String, stackTrace: String?, details: Map<String, Any>, project: Project?, useFrequencyDetector: Boolean = true) {
        if (message.isNullOrEmpty() && stackTrace.isNullOrEmpty()) {
            reportError(
                project, "At least one of the following properties must be set: [message] or [stackTrace].", "reportError",
                mapOf(
                    SEVERITY_PROP_NAME to SEVERITY_HIGH_TRY_FIX
                )
            )
            return
        }
        if (useFrequencyDetector && isTooFrequent(message, stackTrace)) {
            return
        }
        val projectToUse = project ?: findActiveProject()

        projectToUse?.let {
            if (it.isDisposed) return
            ActivityMonitor.getInstance(it).registerError(null, message, details)
        }
    }

    //this method is used to report an error that is not an exception. it should contain some details to say what the error is
    open fun reportError(project: Project?, message: String, action: String, details: Map<String, String>) {


        if (frequencyDetector.isTooFrequentError(message, action)) {
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


    open fun reportError(project: Project?, message: String, throwable: Throwable, details: Map<String, String>) {

        try {
            //many times the exception is no-connection exception, and that may happen too many times.
            // when there is no connection all timers will get an AnalyticsService exception every 10 seconds, it's useless
            //to report that. AnalyticsService exceptions are reported separately and will include no-connection exceptions.
            if (ExceptionUtils.isAnyConnectionException(throwable) || throwable is NoSelectedEnvironmentException) {
                return
            }

            if (frequencyDetector.isTooFrequentException(message, throwable)) {
                return
            }

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


    open fun reportAnalyticsServiceError(
        project: Project,
        message: String,
        methodName: String,
        exception: Exception,
        isConnectionException: Boolean
    ) {

        try {
            if (frequencyDetector.isTooFrequentException(message, exception)) {
                return
            }

            if (!isProjectValid(project)) {
                return
            }

            ActivityMonitor.getInstance(project).registerAnalyticsServiceError(exception, message, methodName, isConnectionException)

        } catch (e: Exception) {
            Log.warnWithException(logger, e, "error in error reporter")
        }
    }


    open fun reportBackendError(message: String, action: String) {
        reportBackendError(findActiveProject(), message, action)
    }

    open fun reportBackendError(project: Project?, message: String, action: String) {
        if (frequencyDetector.isTooFrequentError(message, action)) {
            return
        }

        val projectToUse = project ?: findActiveProject()

        projectToUse?.let {
            ActivityMonitor.getInstance(it).reportBackendError(message, action)
        }
    }


    //better to use overloaded method that accepts project
    open fun reportInternalFatalError(source: String, exception: Throwable, details: Map<String, String> = mapOf()) {
        //todo: change ActivityMonitor to application service
        val projectToUse = findActiveProject() ?: return
        reportInternalFatalError(projectToUse, source, exception, details)
    }

    //this error should be reported only when it's a fatal error that we must fix quickly.
    //don't use it for all errors.
    //currently will be reported from EDT.assertNonDispatchThread and ReadActions.assertNotInReadAccess
    // which usually should be caught in development but if not, are very urgent to fix.
    // if the error is not a result of an exception create a new RuntimeException and send it, so we have the stack trace.
    open fun reportInternalFatalError(project: Project, source: String, exception: Throwable, details: Map<String, String> = mapOf()) {

        if (frequencyDetector.isTooFrequentException(source, exception)) {
            return
        }

        ActivityMonitor.getInstance(project).registerInternalFatalError(source, exception, details)

    }

}

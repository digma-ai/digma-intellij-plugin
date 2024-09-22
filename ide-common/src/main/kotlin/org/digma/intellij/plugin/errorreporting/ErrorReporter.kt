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
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration


const val SEVERITY_PROP_NAME = "severity"
const val SEVERITY_LOW = "low"
const val SEVERITY_MEDIUM = "medium"
const val SEVERITY_HIGH = "high"
const val SEVERITY_DEFAULT = SEVERITY_HIGH

//class and all public methods should be open so that NoOpProxy will work
open class ErrorReporter {

    private val logger: Logger = Logger.getInstance(this::class.java)

    private val frequencyDetector = FrequencyDetector(3.hours.toJavaDuration())

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
    open fun reportError(message: String, t: Throwable, extraDetails: Map<String, Any>) {
        reportError(findActiveProject(), message, t, extraDetails)
    }

    open fun reportError(project: Project?, message: String, throwable: Throwable) {
        reportError(project, message, throwable, mapOf())
    }


    open fun reportUIError(project: Project?, message: String, stackTrace: String?, details: Map<String, Any>) {

        try {
            if (message.isEmpty() && stackTrace.isNullOrEmpty()) {
                reportError(
                    project, "At least one of the following properties must be set: [message] or [stackTrace].", "reportError",
                    mapOf(SEVERITY_PROP_NAME to SEVERITY_HIGH)
                )
                return
            }

            val frequency = frequencyDetector.getStackTraceFrequency(message, stackTrace)
            if (frequency.isTooFrequent()) {
                return
            }

            val projectToUse = project ?: findActiveProject()

            projectToUse?.let {
                if (it.isDisposed) return

                val detailsWithFrequency = details.toMutableMap()
                detailsWithFrequency["frequency"] = frequency.frequencySinceStart
                detailsWithFrequency["frequency.since.minutes"] = frequency.formatDurationToMinutes()

                ActivityMonitor.getInstance(it).registerError(null, message, ensureDetailsWithSeverity(detailsWithFrequency))
            }
        } catch (e: Exception) {
            Log.warnWithException(logger, e, "error in error reporter")
        }
    }

    //this method is used to report an error that is not an exception. it should contain some details to say what the error is
    open fun reportError(message: String, action: String, details: Map<String, Any>) {
        reportError(null, message, action, details)
    }

    //this method is used to report an error that is not an exception. it should contain some details to say what the error is
    open fun reportError(project: Project?, message: String, action: String, details: Map<String, Any>) {
        reportErrorImpl(project, message, action, details)
    }

    open fun reportErrorSkipFrequencyCheck(project: Project?, message: String, action: String, details: Map<String, Any>) {
        reportErrorImpl(project, message, action, details, true)
    }

    private fun reportErrorImpl(project: Project?, message: String, action: String, details: Map<String, Any>, skipFrequencyCheck: Boolean = false) {

        try {

            val frequency = frequencyDetector.getErrorFrequency(message, action)
            if (frequency.isTooFrequent() && !skipFrequencyCheck) {
                return
            }

            val projectToUse = project ?: findActiveProject()

            projectToUse?.let {
                if (it.isDisposed) return

                val detailsToSend = details.toMutableMap()
                detailsToSend["action"] = action
                detailsToSend["frequency"] = frequency.frequencySinceStart
                detailsToSend["frequency.since.minutes"] = frequency.formatDurationToMinutes()

                ActivityMonitor.getInstance(it).registerError(null, message, ensureDetailsWithSeverity(detailsToSend))
            }
        } catch (e: Exception) {
            Log.warnWithException(logger, e, "error in error reporter")
        }
    }


    open fun reportError(project: Project?, message: String, throwable: Throwable, details: Map<String, Any>) {

        try {
            //many times the exception is no-connection exception, and that may happen too many times.
            // when there is no connection all timers will get an AnalyticsService exception every 10 seconds, it's useless
            //to report that. AnalyticsService exceptions are reported separately and will include no-connection exceptions.
            if (ExceptionUtils.isAnyConnectionException(throwable) || throwable is NoSelectedEnvironmentException) {
                return
            }

            val frequency = frequencyDetector.getExceptionFrequency(message, throwable)
            if (frequency.isTooFrequent()) {
                return
            }


            val projectToUse = project ?: findActiveProject()

            projectToUse?.let {
                if (it.isDisposed) return

                val detailsWithFrequency = details.toMutableMap()
                detailsWithFrequency["frequency"] = frequency.frequencySinceStart
                detailsWithFrequency["frequency.since.minutes"] = frequency.formatDurationToMinutes()

                ActivityMonitor.getInstance(it).registerError(throwable, message, ensureDetailsWithSeverity(detailsWithFrequency))
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
            val frequency = frequencyDetector.getExceptionFrequency(message, exception)
            if (frequency.isTooFrequent()) {
                return
            }

            if (!isProjectValid(project)) {
                return
            }

            ActivityMonitor.getInstance(project).registerAnalyticsServiceError(exception, message, methodName, isConnectionException, frequency)

        } catch (e: Exception) {
            Log.warnWithException(logger, e, "error in error reporter")
        }
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


    private fun ensureDetailsWithSeverity(details: Map<String, Any>): Map<String, Any> {
        return if (details.containsKey(SEVERITY_PROP_NAME)) {
            details
        } else {
            val detailsWithSeverity = details.toMutableMap()
            detailsWithSeverity[SEVERITY_PROP_NAME] = SEVERITY_DEFAULT
            detailsWithSeverity
        }
    }

}

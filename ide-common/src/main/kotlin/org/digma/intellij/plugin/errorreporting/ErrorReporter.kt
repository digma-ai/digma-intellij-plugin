package org.digma.intellij.plugin.errorreporting

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.UntraceableException
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.lang.reflect.Field
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Service(Service.Level.APP)
class ErrorReporter {


    companion object {

        @JvmStatic
        fun getInstance(): ErrorReporter {
            return service<ErrorReporter>()
        }
    }


    fun reportError(message: String, t: Throwable) {
        reportError(ProjectUtil.getActiveProject(), message, t)
    }


    fun reportError(project: Project?, message: String, t: Throwable) {
        if (isTooFrequentException(message, t)) {
            return
        }

        //todo: change ActivityMonitor to application service

        val projectToUse = project ?: ProjectUtil.getActiveProject()

        projectToUse?.let {
            ActivityMonitor.getInstance(it).registerError(t, message)
        }

    }


    private fun isTooFrequentException(message: String, t: Throwable): Boolean {
        val hash = computeAccurateTraceHashCode(t)
        val counter = MyCache.getOrCreate(hash, t, message)
        val occurrences = counter.incrementAndGet()
        return occurrences != 1
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
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build<String, AtomicInteger>()

    fun getOrCreate(hash: Int, t: Throwable, message: String): AtomicInteger {
        return cache.get("$hash:$t:$message") { AtomicInteger() }
    }
}

package org.digma.intellij.plugin.scheduling

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.util.concurrency.AppExecutorUtil
import org.apache.commons.lang3.time.StopWatch
import org.digma.intellij.plugin.common.FrequencyDetector
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

val logger = Logger.getInstance("org.digma.scheduler")

//private val scheduler = com.intellij.concurrency.JobScheduler.getScheduler()
private val scheduler = AppExecutorUtil.createBoundedScheduledExecutorService("Digma-scheduler", 3)
private val executor = AppExecutorUtil.createBoundedApplicationPoolExecutor("Digma-one-shot", 3)
private val scheduledTasksPerformanceMonitor = ScheduledTasksPerformanceMonitor()

/**
 * Note: these schedulers are meant for short running tasks of milliseconds, long-running tasks will cause thread starvation.
 * for long-running tasks please use org.digma.intellij.plugin.common.Backgroundable
 */

/*
to call from java do that, assuming a class implements Disposable

        disposingPeriodicTask(this, "aaa", 100, new Function0<Unit>() {
            @Override
            public Unit invoke() {
                return null;
            }
        })
 */




/**
 * starts a periodic task , canceling the task when Disposable is disposed.
 * returns true if the task was registered, false otherwise.
 */
fun Disposable.disposingPeriodicTask(name: String, period: Long, block: () -> Unit): Boolean {

    try {
        val future = scheduler.scheduleWithFixedDelay({
            val stopWatch = StopWatch.createStarted()
            try {
                Log.log(logger::trace, "executing periodic task {}", name)
                block.invoke()
                Log.log(logger::trace, "periodic task {} completed", name)
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "periodic task {} failed", name)
                throw e
            } finally {
                stopWatch.stop()
                scheduledTasksPerformanceMonitor.reportPerformance(name, stopWatch.time)
            }

        }, 0, period, TimeUnit.MILLISECONDS)

        Disposer.register(this) {
            Log.log(logger::trace, "disposing periodic task {}", name)
            future.cancel(true)
        }
        return true

    } catch (e: Throwable) {
        Log.warnWithException(logger, e, "could not schedule periodic task {}", name)
        ErrorReporter.getInstance().reportError("org.digma.scheduler.disposingPeriodicTask", e)
        return false
    }
}


/**
 * executes a task with possible result or Unit, not waiting for the task to finish ,
 * returning a future that can be used to wait for completion or cancel the task.
 * the future will be null in case that task could not be submitted.
 * canceling doesn't mean the task will abort, the code needs to check interrupted flag.
 */
fun <T> oneShotTask(name: String, block: () -> T): Future<T>? {

    try {
        return executor.submit(Callable {
            try {
                Log.log(logger::trace, "executing one-shot task {}", name)
                val result = block.invoke()
                Log.log(logger::trace, "one-shot task {} completed", name)
                return@Callable result
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "one-shot task {} failed", name)
                ErrorReporter.getInstance().reportError("org.digma.scheduler.oneShotTask", e)
                throw e
            }
        })

    } catch (e: Throwable) {
        Log.warnWithException(logger, e, "could not submit one shot task {}", name)
        ErrorReporter.getInstance().reportError("org.digma.scheduler.oneShotTask", e)
        return null
    }
}

/**
 * executes a task with no result , waiting for the task to finish within timeout,
 * returns true if the task completed successfully.
 */
fun oneShotTask(name: String, timeoutMillis: Long, block: () -> Unit): Boolean {

    try {
        val future = executor.submit {
            Log.log(logger::trace, "executing one-shot task {}", name)
            block.invoke()
            Log.log(logger::trace, "one-shot task {} completed", name)
        }

        try {
            future.get(timeoutMillis, TimeUnit.MILLISECONDS)
            return true
        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "one-shot task {} failed", name)
            ErrorReporter.getInstance().reportError("org.digma.scheduler.oneShotTask", e)
            return false
        }
    } catch (e: Throwable) {
        Log.warnWithException(logger, e, "could not submit one shot task {}", name)
        ErrorReporter.getInstance().reportError("org.digma.scheduler.oneShotTask", e)
        return false
    }
}


/**
 * executes a task with result , waiting for the task to finish within timeout and return its result.
 * throws a RuntimeException wrapping the original exception if the task fails or the task could not be submitted.
 */
@Throws(RuntimeException::class)
fun <T> oneShotTaskWithResult(name: String, timeoutMillis: Long, block: () -> T): T {

    try {

        val future = executor.submit(Callable {
            Log.log(logger::trace, "executing one-shot task with result {}", name)
            val result = block.invoke()
            Log.log(logger::trace, "one-shot task with result {} completed", name)
            return@Callable result
        })

        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS)
        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "one-shot task with result {} failed", name)
            ErrorReporter.getInstance().reportError("org.digma.scheduler.oneShotTaskWithResult", e)
            throw RuntimeException(e)
        }
    } catch (e: Throwable) {
        Log.warnWithException(logger, e, "could not submit one shot task {}", name)
        ErrorReporter.getInstance().reportError("org.digma.scheduler.oneShotTaskWithResult", e)
        throw RuntimeException(e)
    }
}


class ScheduledTasksPerformanceMonitor {

    private val frequencyDetector = FrequencyDetector(30.minutes.toJavaDuration())

    //this is raw sampling report, not all issues are reported,
    // and it may be that we miss some issues if they don't happen all the time.
    //if a task is always slow it will be reported eventually
    fun reportPerformance(taskName: String, duration: Long) {
        try {

            if (duration < 2000) {
                return
            }

            if (frequencyDetector.isTooFrequentMessage(taskName)) {
                return
            }

            findActiveProject()?.let {
                val details = mutableMapOf<String, Any>(
                    "task name" to taskName,
                    "duration" to duration
                )

                ActivityMonitor.getInstance(it).reportScheduledTaskPerformanceIssue(details)
            }
        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "error in reportPerformance")
        }
    }

}
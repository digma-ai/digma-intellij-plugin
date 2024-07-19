@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.scheduling

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer
import com.intellij.util.concurrency.AppExecutorUtil
import com.jetbrains.rd.util.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.commons.lang3.time.StopWatch
import org.digma.intellij.plugin.common.FrequencyDetector
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration


/**
 * Note: these schedulers are meant to serve a low number of short running tasks that take milliseconds or just a few seconds, period should
 * be 10 seconds or more.
 * long-running tasks will cause thread starvation. too many tasks will also cause thread starvation. too short period will also cause thread starvation.
 * we don't have a lot of these tasks, we have a few dozens. the management task limits it to SCHEDULER_MAX_REGISTERED_TASKS and reports an error if more are registered.
 * for long-running tasks please use org.digma.intellij.plugin.common.Backgroundable.
 * one-shot tasks support is also meant for low number of tasks that run very fast, not more than milliseconds. we don't have too many.
 */

/*
to call from java , assuming a class implements Disposable, do that:

        disposingPeriodicTask(this, "aaa", 100, new Function0<String>() {
            @Override
            public String invoke() {
                return "aaa";
            }
        })
 */


const val INITIAL_SCHEDULER_CORE_SIZE = 2
const val SCHEDULER_MAX_SIZE = 5 // 5 should be enough , we don't have too many tasks,if we reached this max we have thread starvation
const val SCHEDULER_MAX_REGISTERED_TASKS = 100


val logger = Logger.getInstance("org.digma.scheduler")

private val scheduler: MyScheduledExecutorService = MyScheduledExecutorService("Digma-Scheduler", INITIAL_SCHEDULER_CORE_SIZE)

//todo: implement like MyScheduledExecutorService,currently using intellij executor
private val executor: ExecutorService = AppExecutorUtil.createBoundedApplicationPoolExecutor("Digma-one-shot", 3)
private val scheduledTasksPerformanceMonitor = ScheduledTasksPerformanceMonitor()

//it's not private so that it can be used in unit tests
fun manage() {
    scheduler.manage()
}


//this service is necessary to shut down the thread pools, its getInstance is
// called in ProjectActivity to initialize it, and dispose is called on application shutdown.
//also used to run a management task.
@Service(Service.Level.APP)
class ThreadPoolProviderService : Disposable {
    companion object {
        fun getInstance(): ThreadPoolProviderService {
            return service<ThreadPoolProviderService>()
        }
    }

    init {
        disposingScope().launch {

            //wait 2 minutes, let the pool start working before the first management
            delay(2.minutes.inWholeMilliseconds)

            val delay = 2.minutes.inWholeMilliseconds
            while (isActive) {
                try {
                    manage()
                    //the pool size was increased to SCHEDULER_MAX_SIZE, we have a problem, report an error
                    if (scheduler.corePoolSize >= SCHEDULER_MAX_SIZE) {
                        ErrorReporter.getInstance().reportError(
                            "ThreadPoolProviderService.manage", "scheduler max size reached", mapOf(
                                "core.pool.size" to scheduler.corePoolSize,
                                "max.pool.size" to scheduler.maximumPoolSize,
                                "max.pool.size.allowed" to SCHEDULER_MAX_SIZE
                            )
                        )
                    }

                    //this scheduler can not be used for high load
                    if (scheduler.registeredTasks.get() >= SCHEDULER_MAX_REGISTERED_TASKS) {
                        ErrorReporter.getInstance().reportError(
                            "ThreadPoolProviderService.manage", "too many registered tasks in scheduler", mapOf(
                                "core.pool.size" to scheduler.corePoolSize,
                                "max.pool.size" to scheduler.maximumPoolSize,
                                "registered.tasks" to scheduler.registeredTasks.get(),
                                "max.registered.allowed" to SCHEDULER_MAX_REGISTERED_TASKS
                            )
                        )
                    }

                    Log.log(
                        logger::trace,
                        "scheduler statistics: core pool size:{},registered tasks:{}",
                        scheduler.corePoolSize,
                        scheduler.registeredTasks
                    )
                    delay(delay)
                } catch (e: Throwable) {
                    ErrorReporter.getInstance().reportError("ThreadPoolProviderService.manage", e)
                }
            }
        }
    }

    override fun dispose() {
        try {
            scheduler.shutdownNow()
        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "error in dispose")
        }
        try {
            executor.shutdownNow()
        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "error in dispose")
        }
    }
}

class ThreadPoolProviderServiceStarter : ProjectActivity {
    override suspend fun execute(project: Project) {
        ThreadPoolProviderService.getInstance()
    }
}


/**
 * starts a periodic task , canceling the task when Disposable is disposed.
 * returns true if the task was registered, false otherwise.
 */
fun Disposable.disposingPeriodicTask(name: String, period: Long, block: () -> Unit): Boolean {

    try {
        //catch and swallow all exceptions. there is no point in throwing them.
        // also some implementations will cancel the task if it throws an exception, we don't want that
        // because our tasks may throw exceptions. usually we handle exceptions in the task body,
        // but if not then exceptions will be caught here and logged.
        val future = scheduler.scheduleWithFixedDelay({
            val stopWatch = StopWatch.createStarted()
            try {
                Log.log(logger::trace, "executing periodic task {}", name)
                block.invoke()
                Log.log(logger::trace, "periodic task {} completed", name)
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "periodic task {} failed", name)
                ErrorReporter.getInstance().reportError("org.digma.scheduler.disposingPeriodicTask", e)
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
            val stopWatch = StopWatch.createStarted()
            try {
                Log.log(logger::trace, "executing one-shot task {}", name)
                val result = block.invoke()
                Log.log(logger::trace, "one-shot task {} completed", name)
                return@Callable result
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "one-shot task {} failed", name)
                ErrorReporter.getInstance().reportError("org.digma.scheduler.oneShotTask", e)
                throw e
            } finally {
                stopWatch.stop()
                scheduledTasksPerformanceMonitor.reportPerformance(name, stopWatch.time)
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
            val stopWatch = StopWatch.createStarted()
            try {
                Log.log(logger::trace, "executing one-shot task {}", name)
                block.invoke()
                Log.log(logger::trace, "one-shot task {} completed", name)
            } finally {
                stopWatch.stop()
                scheduledTasksPerformanceMonitor.reportPerformance(name, stopWatch.time)
            }
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
            val stopWatch = StopWatch.createStarted()
            try {
                Log.log(logger::trace, "executing one-shot task with result {}", name)
                val result = block.invoke()
                Log.log(logger::trace, "one-shot task with result {} completed", name)
                return@Callable result
            } finally {
                stopWatch.stop()
                scheduledTasksPerformanceMonitor.reportPerformance(name, stopWatch.time)
            }
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


class MyScheduledExecutorService(
    threadsNames: String, corePoolSize: Int
) : ScheduledThreadPoolExecutor(corePoolSize, CountingThreadFactory(threadsNames)) {

    var registeredTasks = AtomicInteger(0)
    private var managementRound = 0
    private var exhaustedCount = AtomicInteger(0)

    init {
        continueExistingPeriodicTasksAfterShutdownPolicy = false
        executeExistingDelayedTasksAfterShutdownPolicy = false
        removeOnCancelPolicy = true
        maximumPoolSize = max(SCHEDULER_MAX_SIZE, corePoolSize)
    }

    override fun scheduleWithFixedDelay(command: Runnable, initialDelay: Long, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
        registeredTasks.incrementAndGet()
        return super.scheduleWithFixedDelay(command, initialDelay, delay, unit)
    }


//    override fun beforeExecute(t: Thread?, r: Runnable?) {
//        super.beforeExecute(t, r)
//    }
//
//    override fun afterExecute(r: Runnable?, t: Throwable?) {
//        super.afterExecute(r, t)
//    }

    fun manage() {
        managementRound++

        val poolSize = scheduler.corePoolSize
        val maxPoolSize = scheduler.maximumPoolSize
        val activeCount = scheduler.activeCount

        if (activeCount >= poolSize) exhaustedCount.incrementAndGet()

        //check every 5 rounds, if was always exhausted increase pool size
        if (managementRound % 5 == 0) {
            val exhausted = exhaustedCount.incrementAndGet()
            exhaustedCount.set(0)
            if (exhausted >= 5) {
                corePoolSize = min(maxPoolSize, poolSize + 1)
                Log.log(logger::trace, "scheduler pool size increased to {}", corePoolSize)
            }
        }
    }
}


class CountingThreadFactory(private val baseThreadName: String) : ThreadFactory {
    private val myFactory = Executors.defaultThreadFactory()
    private val counter = AtomicInteger()
    override fun newThread(r: Runnable): Thread {
        val t = myFactory.newThread(r)
        t.isDaemon = true
        t.name = "$baseThreadName-${counter.incrementAndGet()}"
        return t
    }
}
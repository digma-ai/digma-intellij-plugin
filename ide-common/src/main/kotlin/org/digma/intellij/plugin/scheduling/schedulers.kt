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

/*
 NOTE: important to remember: code that will call digma API should be executed with one of the schedulers in this file and not in a coroutine.
    while it is possible and will work it can get into trouble in certain situations. and anyway has no advantage over using the schedulers
    in this file.
 */

/**
 * Note: these schedulers are meant to serve a low number of short running tasks that take milliseconds or just a few seconds, period should
 * be 10 seconds or more.
 * long-running tasks will cause thread starvation. too many tasks will also cause thread starvation. too short period will also cause thread starvation.
 * we don't have a lot of these tasks, we have a few dozens per project. the management task limits it to SCHEDULER_MAX_REGISTERED_TASKS and reports
 * an error if more are registered.
 * for long-running tasks please use org.digma.intellij.plugin.common.Backgroundable.
 * one-shot tasks support is also meant for low number of tasks that run very fast, not more than milliseconds. we don't have too many.
 */


/*
 When to use these schedulers vs kotlin coroutine disposingScope:
 it is possible to implement a recurring task using kotlin coroutines patterns:
 disposingScope().launch {
    while(isActive){
        do something
        delay(10000)
    }
 }

 we realized that using disposingScope coroutine has some disadvantages:
 * the code runs in a scope of coroutine, this poses some limitations, for example if somewhere in the call stack
    needs to call a suspending function and wait for it to finish the most useful way is using runBlocking, but runBlocking can not be called in a scope
    of another coroutine. it makes it impossible to do that.
 * there is no point in starting a coroutine if the executing code is not a suspending code.
 * in the pattern shown above with disposingScope the code needs to manage a while loop, a delay, exception handling, and always consider
    the fact that the code runs in a scope of a coroutine.
 * there is no central management for the tasks, for example it's very difficult to report statistics about all running tasks.

using the scheduler in this file is cleaner:
 disposingPeriodicTask("test", 100) {
       do something
 }

the scheduler is a central management point, it can handle timeouts, cancellation, error handling, statistics.
the code doesn't need to maintain a while loop, delays, timeouts,and error handling.
the scheduler can report statistics and performance of running tasks.


How and when to use the schedulers in this file:

if you need a recurring task that will run for the lifetime of the project or application or a local disposable, it should execute periodically in predefined
intervals. use disposingPeriodicTask.
disposingPeriodicTask is meant for short running tasks with intervals that are few seconds or more. don't use it for long-running tasks or
with very short intervals, a 10 seconds or more interval is recommended. very short intervals will cause high load or thread starvation,
this is not the facility for such requirements.

if you need a one-shot very short running task that must run on a background thread, use one of the oneShotXXX task methods.
see org.digma.intellij.plugin.scheduling.SchedulingTests for examples.


when to use disposingScope().launch :
if disposing of the recurring task has more complex conditions. the schedulers here have one option to stop the recurring task when
the parent disposable is disposed. if stopping the task has more conditions that are known only to the executed code then probably
the coroutine pattern is more conformable. or a regular java java.util.TimerTask.
see for example org.digma.intellij.plugin.ui.recentactivity.RecentActivityService.openRegistrationDialog, it is waiting with a while loop
for the app to initialize, with a scheduler we don't know how long it will take , a recurring task of 5 millis may create high load
on the scheduler. this is a classic case where a local coroutine is more comfortable than a scheduler.

if you need to execute suspending code it's better to use a coroutine.


So a rule of thumb may be:
always prefer a scheduler when possible.

if the task can be disposed by a parent disposable, is very short: use a scheduler with a correct parent disposable,
usually a project service or a local disposable.

if the task has more complex stopping conditions that are known only to the executed code and can not be used with a disposable.
or if the task interval may change depending on some variables: use disposingScope().launch

 */

/*
Cancellation:
when a one-shot task fails on timeout it will be canceled. user code needs to check InterruptedException and the interrupted flag in order
to quite the task.
see java doc of InterruptedException.
it is similar to cancellation exception in kotlin coroutines.
 */


/*
how to dispose a task using a local disposable, in case the task should be disposed early not related to project closing:

see example in ide-common/src/main/kotlin/org/digma/intellij/plugin/posthog/PerformanceMetricsPosthogEventStartupActivity.kt

val disposable = Disposer.newDisposable()
disposable.disposingPeriodicTask("task-name",2.minutes.inWholeMilliseconds,10.minutes.inWholeMilliseconds){
    if (need to dispose the task) {
        Disposer.dispose(disposable)
    }else {
        do something
    }
}
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
const val SCHEDULER_MAX_REGISTERED_TASKS = 200


val logger = Logger.getInstance("org.digma.scheduler")

private val scheduler: MyScheduledExecutorService = MyScheduledExecutorService("Digma-Scheduler", INITIAL_SCHEDULER_CORE_SIZE)

//todo: implement like MyScheduledExecutorService,currently using intellij executor, or use intellij's own thread pool which is unbounded
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
                    if (scheduler.registeredRecurringTasks.get() >= SCHEDULER_MAX_REGISTERED_TASKS) {
                        ErrorReporter.getInstance().reportError(
                            "ThreadPoolProviderService.manage", "too many registered tasks in scheduler", mapOf(
                                "core.pool.size" to scheduler.corePoolSize,
                                "max.pool.size" to scheduler.maximumPoolSize,
                                "registered.tasks" to scheduler.registeredRecurringTasks.get(),
                                "max.registered.allowed" to SCHEDULER_MAX_REGISTERED_TASKS
                            )
                        )
                    }

                    Log.log(
                        logger::trace,
                        "management: scheduler statistics: core pool size:{},registered tasks:{}",
                        scheduler.corePoolSize,
                        scheduler.registeredRecurringTasks
                    )
                    delay(2.minutes.inWholeMilliseconds)
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
    return disposingPeriodicTask(name, 0, period, block)
}

/**
 * starts a periodic task , canceling the task when Disposable is disposed.
 * wait startupDelay before first execution.
 * returns true if the task was registered, false otherwise.
 */
fun Disposable.disposingPeriodicTask(name: String, startupDelay: Long, period: Long, block: () -> Unit): Boolean {

    Log.log(logger::trace, "registering disposingPeriodicTask {}, startupDelay:{},period:{}", name, startupDelay, period)

    val future = try {
        //catch and swallow all exceptions. there is no point in throwing them.
        // also some implementations will cancel the task if it throws an exception, we don't want that
        // because our tasks may throw exceptions. usually we handle exceptions in the task body,
        // but if not then exceptions will be caught here and logged.
        scheduler.scheduleWithFixedDelay({
            val stopWatch = StopWatch.createStarted()
            try {
                Log.logWithThreadName(logger::trace, "executing periodic task {}", name)
                block.invoke()
                Log.logWithThreadName(logger::trace, "periodic task {} completed", name)
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "periodic task {} failed", name)
                ErrorReporter.getInstance().reportError("org.digma.scheduler.disposingPeriodicTask", e)
            } finally {
                stopWatch.stop()
                scheduledTasksPerformanceMonitor.reportPerformance(name, stopWatch.time)
            }

        }, startupDelay, period, TimeUnit.MILLISECONDS)
    } catch (e: Throwable) {
        Log.warnWithException(logger, e, "could not schedule periodic task {}", name)
        ErrorReporter.getInstance().reportError("org.digma.scheduler.disposingPeriodicTask", e)
        return false
    }

    Disposer.register(this) {
        Log.log(logger::trace, "disposing periodic task {}", name)
        future.cancel(true)
    }
    return true


}

/**
 * executes a one-shot task that will run after the given delay.
 * returns true if the task was registered, false otherwise.
 */
fun Disposable.disposingOneShotDelayedTask(name: String, delay: Long, block: () -> Unit): Boolean {

    /*
    This comes to avoid memory leaks.
    consider this:
    calling disposingOneShotDelayedTask on a Disposable ,lets say a project service. and we would just register
    a child disposable on the project service to cancel the task.
    if the project is closed before the task is executed the parent disposable, the service, will be disposed with all its children
    and the task will be canceled.
    but if the task did execute and the project service was not disposed yet, we are left with a disposable child that will be disposed
    only when the service is disposed.
    it's not such a big deal, its just another disposable in some map. but if many tasks are registered on the same project service
    we end up with many children still registered while actually all the tasks were executed already.

    the solution:
    calling disposingOneShotDelayedTask on a Disposable project service. the parent.
    we create here a new disposable,the child, register it as child of the parent.
    register the task on the child disposable and in disposingOneShotDelayedTask0 we register a child for the child.
    if the parent is disposed before the task is executed, the first child will be disposed, its child will be disposed and the task
    will be canceled.
    if the task executed and completed, the task itself disposes the first child, which will dispose its child. and now the parent
    doesn't have a leftover child.

    Notice that we execute disposingOneShotDelayedTask0 on the child disposable, its private and should only be used internally.
    in disposingOneShotDelayedTask0 when the task completes it disposes this in the finally block.
    see unit tests:
    testDisposingOneShotDelayedTaskParentDisposableHasNoChildren
    testDisposingOneShotDelayedTaskCanceledParentDisposableHasNoChildren

     */

    val disposable = Disposer.newDisposable()
    Disposer.register(this, disposable)
    return disposable.disposingOneShotDelayedTask0(name, delay, block)
}

private fun Disposable.disposingOneShotDelayedTask0(name: String, delay: Long, block: () -> Unit): Boolean {

    Log.log(logger::trace, "registering disposingOneShotDelayedTask {}, delay:{}", name, delay)

    val future = try {
        //catch and swallow all exceptions. there is no point in throwing them.
        // also some implementations will cancel the task if it throws an exception, we don't want that
        // because our tasks may throw exceptions. usually we handle exceptions in the task body,
        // but if not then exceptions will be caught here and logged.
        scheduler.schedule({

            val stopWatch = StopWatch.createStarted()
            try {
                Log.logWithThreadName(logger::trace, "executing delayed task {}", name)
                block.invoke()
                Log.logWithThreadName(logger::trace, "delayed task {} completed", name)
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "delayed task {} failed", name)
                ErrorReporter.getInstance().reportError("org.digma.scheduler.disposingOneShotDelayedTask", e)
            } finally {
                stopWatch.stop()
                scheduledTasksPerformanceMonitor.reportPerformance(name, stopWatch.time)
                Disposer.dispose(this)
            }

        }, delay, TimeUnit.MILLISECONDS)
    } catch (e: Throwable) {
        Log.warnWithException(logger, e, "could not schedule delayed task {}", name)
        ErrorReporter.getInstance().reportError("org.digma.scheduler.disposingOneShotDelayedTask", e)
        return false
    }


    //this will run when parent disposable is disposed which will dispose this disposable and its children
    Disposer.register(this) {
        Log.log(logger::trace, "disposing delayed task {}", name)
        future.cancel(true)
    }

    return true
}

/**
 * executes a task with possible result or Unit, not waiting for the task to finish ,
 * returning a future that can be used to wait for completion or cancel the task.
 * the future will be null in case that task could not be submitted.
 * canceling doesn't mean the task will abort, the code needs to check interrupted flag.
 */
fun <T> oneShotTask(name: String, block: () -> T): Future<T>? {

    Log.log(logger::trace, "registering oneShotTask {}", name)

    try {
        return executor.submit(Callable {
            val stopWatch = StopWatch.createStarted()
            try {
                Log.logWithThreadName(logger::trace, "executing one-shot task {}", name)
                val result = block.invoke()
                Log.logWithThreadName(logger::trace, "one-shot task {} completed", name)
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

    Log.log(logger::trace, "registering oneShotTask {}, timeoutMillis:{}", name, timeoutMillis)

    val future = try {
        executor.submit {
            val stopWatch = StopWatch.createStarted()
            try {
                Log.logWithThreadName(logger::trace, "executing one-shot task {}", name)
                block.invoke()
                Log.logWithThreadName(logger::trace, "one-shot task {} completed", name)
            } finally {
                stopWatch.stop()
                scheduledTasksPerformanceMonitor.reportPerformance(name, stopWatch.time)
            }
        }
    } catch (e: Throwable) {
        Log.warnWithException(logger, e, "could not submit one shot task {}", name)
        ErrorReporter.getInstance().reportError("org.digma.scheduler.oneShotTask", e)
        return false
    }

    try {
        future.get(timeoutMillis, TimeUnit.MILLISECONDS)
        return true
    } catch (e: Throwable) {
        future.cancel(true)
        Log.warnWithException(logger, e, "one-shot task {} failed", name)
        ErrorReporter.getInstance().reportError("org.digma.scheduler.oneShotTask", e)
        return false
    }

}


/**
 * executes a task with result , waiting for the task to finish within timeout and return its result.
 * throws a RuntimeException wrapping the original exception if the task fails or the task could not be submitted.
 */
@Throws(Exception::class)
fun <T> oneShotTaskWithResult(name: String, timeoutMillis: Long, block: () -> T): T {

    Log.log(logger::trace, "registering oneShotTaskWithResult {}, timeoutMillis:{}", name, timeoutMillis)

    val future = try {

        executor.submit(Callable {
            val stopWatch = StopWatch.createStarted()
            try {
                Log.logWithThreadName(logger::trace, "executing one-shot task with result {}", name)
                val result = block.invoke()
                Log.logWithThreadName(logger::trace, "one-shot task with result {} completed", name)
                return@Callable result
            } finally {
                stopWatch.stop()
                scheduledTasksPerformanceMonitor.reportPerformance(name, stopWatch.time)
            }
        })
    } catch (e: Throwable) {
        Log.warnWithException(logger, e, "could not submit one shot task {}", name)
        ErrorReporter.getInstance().reportError("org.digma.scheduler.oneShotTaskWithResult", e)
        throw e
    }

    try {
        return future.get(timeoutMillis, TimeUnit.MILLISECONDS)
    } catch (e: Throwable) {
        future.cancel(true)
        Log.warnWithException(logger, e, "one-shot task with result {} failed", name)
        ErrorReporter.getInstance().reportError("org.digma.scheduler.oneShotTaskWithResult", e)
        throw e
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

    var registeredRecurringTasks = AtomicInteger(0)
    private var managementRound = 0
    private var exhaustedCount = AtomicInteger(0)

    init {
        continueExistingPeriodicTasksAfterShutdownPolicy = false
        executeExistingDelayedTasksAfterShutdownPolicy = false
        removeOnCancelPolicy = true
        maximumPoolSize = max(SCHEDULER_MAX_SIZE, corePoolSize)
    }

    override fun scheduleWithFixedDelay(command: Runnable, initialDelay: Long, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
        registeredRecurringTasks.incrementAndGet()
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

        Log.log(logger::trace, "running management on scheduler")

        val poolSize = scheduler.corePoolSize
        val maxPoolSize = scheduler.maximumPoolSize
        val activeCount = scheduler.activeCount

        Log.log(logger::trace, "management: on scheduler poolSize:{},activeCount:{}", poolSize, activeCount)

        if (activeCount >= poolSize) exhaustedCount.incrementAndGet()

        //check every 5 rounds, if was always exhausted increase pool size
        if (managementRound % 5 == 0) {
            val exhausted = exhaustedCount.get()
            Log.log(logger::trace, "management: on scheduler exhausted:{}", exhausted)
            exhaustedCount.set(0)
            if (exhausted >= 5) {
                corePoolSize = min(maxPoolSize, poolSize + 1)
                Log.log(logger::trace, "management: on scheduler pool size increased to {}", corePoolSize)
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
package org.digma.intellij.plugin.scheduling

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer
import com.jetbrains.rd.util.AtomicInteger
import org.apache.commons.lang3.time.StopWatch
import org.digma.intellij.plugin.analytics.ApiErrorHandler
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
import org.digma.intellij.plugin.common.FrequencyDetector
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.common.generateDeadlockedThreadDump
import org.digma.intellij.plugin.common.generateMonitorDeadlockedThreadDump
import org.digma.intellij.plugin.common.generateThreadDump
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.util.Collections
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.timer
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


const val INITIAL_SCHEDULER_CORE_SIZE = 5

// this max size should be enough , we don't have too many tasks,if we reached this max we have thread starvation. it is reported to posthog when reached.
const val SCHEDULER_MAX_SIZE = 20
const val SCHEDULER_MAX_QUEUE_SIZE_ALLOWED = 200

val logger = Logger.getInstance("org.digma.scheduler")

private val scheduler: MyScheduledExecutorService = MyScheduledExecutorService("Digma-Scheduler", INITIAL_SCHEDULER_CORE_SIZE)

private val scheduledTasksPerformanceMonitor = ScheduledTasksPerformanceMonitor()

//this method is here and not private so that it can be used in unit tests
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

    private val alreadySentSchedulerMaxSize = AtomicBoolean(false)
    private val alreadySentSchedulerMaxRegisteredSize = AtomicBoolean(false)


    private val managementTimer = timer("SchedulerManager", true, 2.minutes.inWholeMilliseconds, 2.minutes.inWholeMilliseconds) {
        try {
            //call the scheduler manage
            manage()

            //the pool size was increased to SCHEDULER_MAX_SIZE, we have a problem, report an error
            if (scheduler.corePoolSize >= SCHEDULER_MAX_SIZE && !alreadySentSchedulerMaxSize.get()) {
                alreadySentSchedulerMaxSize.set(true)
                ErrorReporter.getInstance().reportError(
                    "ThreadPoolProviderService.schedulerMaxSize", "scheduler max size reached", mapOf(
                        "core.pool.size" to scheduler.corePoolSize,
                        "max.pool.size" to scheduler.maximumPoolSize,
                        "task.queue.size" to scheduler.queue.size,
                        "all.registered.recurring (including canceled)" to scheduler.registeredRecurringTasks,
                        "max.pool.size.allowed" to SCHEDULER_MAX_SIZE,
                        "max.queue.size.allowed" to SCHEDULER_MAX_QUEUE_SIZE_ALLOWED,
                        "ThreadDump" to generateThreadDump(),
                        "DeadLockThreadDump" to generateDeadlockedThreadDump(),
                        "MonitorDeadlockedThreadDump" to generateMonitorDeadlockedThreadDump(),
                    )
                )
            }

            //this scheduler can not be used for high load
            if (scheduler.queue.size >= SCHEDULER_MAX_QUEUE_SIZE_ALLOWED && !alreadySentSchedulerMaxRegisteredSize.get()) {
                alreadySentSchedulerMaxRegisteredSize.set(true)
                ErrorReporter.getInstance().reportError(
                    "ThreadPoolProviderService.schedulerMaxRegisteredReached", "too many registered tasks in scheduler", mapOf(
                        "core.pool.size" to scheduler.corePoolSize,
                        "max.pool.size" to scheduler.maximumPoolSize,
                        "task.queue.size" to scheduler.queue.size,
                        "all.registered.recurring (including canceled)" to scheduler.registeredRecurringTasks,
                        "max.pool.size.allowed" to SCHEDULER_MAX_SIZE,
                        "max.queue.size.allowed" to SCHEDULER_MAX_QUEUE_SIZE_ALLOWED
                    )
                )
            }
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("ThreadPoolProviderService.manage", e)
        }
    }


    fun interruptAll() {
        scheduler.interruptAll()
    }


    override fun dispose() {

        managementTimer.cancel()

        try {
            scheduler.shutdownNow()
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

//used to pause recurring tasks
private fun paused(passable: Boolean): Boolean {
    return try {
        //will fail in tests
        passable &&
                (ApiErrorHandler.getInstance().isNoConnectionMode() || DigmaDefaultAccountHolder.getInstance().account == null)
    } catch (e: Throwable) {
        false
    }
}

/**
 * starts a periodic task , canceling the task when Disposable is disposed.
 * returns true if the task was registered, false otherwise.
 */
fun Disposable.disposingPeriodicTask(name: String, period: Long, passable: Boolean, block: () -> Unit): Boolean {
    return disposingPeriodicTask(name, 0, period, passable, block)
}

/**
 * starts a periodic task , canceling the task when Disposable is disposed.
 * wait startupDelay before first execution.
 * returns true if the task was registered, false otherwise.
 */
fun Disposable.disposingPeriodicTask(name: String, startupDelay: Long, period: Long, passable: Boolean, block: () -> Unit): Boolean {

    Log.log(logger::trace, "registering disposingPeriodicTask {}, startupDelay:{},period:{}", name, startupDelay, period)

    val future = try {

        //catch and swallow all exceptions. there is no point in throwing them.
        // also some implementations will cancel the task if it throws an exception, we don't want that
        // because our tasks may throw exceptions, and we want them to keep recurring.
        //usually we handle exceptions in the task body,but if not then exceptions will be caught here and logged.
        scheduler.scheduleWithFixedDelay({

            if (paused(passable)) {
                return@scheduleWithFixedDelay
            }

            val stopWatch = StopWatch.createStarted()
            try {
                Log.logWithThreadName(logger::trace, "executing periodic task {}", name)
                block.invoke()
            } catch (e: InterruptedException) {
                Log.logWithThreadName(logger::trace, "periodic task {} interrupted", name)
                //don't log InterruptedException to posthog, it doesn't help us
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "periodic task {} failed", name)
                ErrorReporter.getInstance().reportError("org.digma.scheduler.disposingPeriodicTask", e, mapOf("task.name" to name))
            } finally {
                stopWatch.stop()
                Log.logWithThreadName(logger::trace, "periodic task {} completed in {} millis", name, stopWatch.time)
                scheduledTasksPerformanceMonitor.reportPerformance(name, stopWatch.time)
            }

        }, startupDelay, period, TimeUnit.MILLISECONDS)

    } catch (e: Throwable) {
        Log.warnWithException(logger, e, "could not schedule periodic task {}", name)
        ErrorReporter.getInstance().reportError("org.digma.scheduler.disposingPeriodicTask", e, mapOf("task.name" to name))
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
    return disposable.disposingOneShotDelayedTaskImpl(name, delay, block)
}

private fun Disposable.disposingOneShotDelayedTaskImpl(name: String, delay: Long, block: () -> Unit): Boolean {

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

            } catch (e: InterruptedException) {
                Log.logWithThreadName(logger::trace, "delayed task {} interrupted", name)
                //don't log InterruptedException to posthog, it doesn't help us
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "delayed task {} failed", name)
                ErrorReporter.getInstance().reportError("org.digma.scheduler.disposingOneShotDelayedTask", e, mapOf("task.name" to name))
            } finally {
                stopWatch.stop()
                Log.logWithThreadName(logger::trace, "delayed task {} completed in {} millis", name, stopWatch.time)
                scheduledTasksPerformanceMonitor.reportPerformance(name, stopWatch.time)
                Disposer.dispose(this)
            }

        }, delay, TimeUnit.MILLISECONDS)

    } catch (e: Throwable) {
        Log.warnWithException(logger, e, "could not schedule delayed task {}", name)
        ErrorReporter.getInstance().reportError("org.digma.scheduler.disposingOneShotDelayedTask", e, mapOf("task.name" to name))
        return false
    }


    //this will run when parent disposable is disposed which will dispose this disposable and its children,if not disposed already.
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

        return scheduler.submit(Callable {
            val stopWatch = StopWatch.createStarted()
            try {
                Log.logWithThreadName(logger::trace, "executing one-shot task {}", name)
                val result = block.invoke()

                return@Callable result
            } catch (e: InterruptedException) {
                Log.logWithThreadName(logger::trace, "one-shot task {} interrupted", name)
                //don't log InterruptedException to posthog, it doesn't help us
                throw e
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "one-shot task {} failed", name)
                ErrorReporter.getInstance().reportError("org.digma.scheduler.oneShotTask", e, mapOf("task.name" to name))
                throw e
            } finally {
                stopWatch.stop()
                Log.logWithThreadName(logger::trace, "one-shot task {} completed in {} millis", name, stopWatch.time)
                scheduledTasksPerformanceMonitor.reportPerformance(name, stopWatch.time)
            }
        })

    } catch (e: Throwable) {
        Log.warnWithException(logger, e, "could not submit one-shot task {}", name)
        ErrorReporter.getInstance().reportError("org.digma.scheduler.oneShotTask", e, mapOf("task.name" to name))
        return null
    }
}

/**
 * executes a task with no result , waiting for the task to finish within timeout,
 * returns true if the task completed successfully.
 */
fun blockingOneShotTask(name: String, timeoutMillis: Long, block: () -> Unit): Boolean {

    Log.log(logger::trace, "registering blockingOneShotTask {}, timeoutMillis:{}", name, timeoutMillis)

    val future = try {

        scheduler.submit {
            val stopWatch = StopWatch.createStarted()
            try {
                Log.logWithThreadName(logger::trace, "executing one-shot task {}", name)
                block.invoke()

            } finally {
                stopWatch.stop()
                Log.logWithThreadName(logger::trace, "one-shot task {} completed in {} millis", name, stopWatch.time)
                scheduledTasksPerformanceMonitor.reportPerformance(name, stopWatch.time)
            }
        }

    } catch (e: Throwable) {
        Log.warnWithException(logger, e, "could not submit one-shot task {}", name)
        ErrorReporter.getInstance().reportError("org.digma.scheduler.blockingOneShotTask", e, mapOf("task.name" to name))
        return false
    }

    try {
        future.get(timeoutMillis, TimeUnit.MILLISECONDS)
        return true
    } catch (e: InterruptedException) {
        Log.logWithThreadName(logger::trace, "one-shot task {} interrupted", name)
        //don't log InterruptedException to posthog, it doesn't help us
        return false
    } catch (e: Throwable) {
        future.cancel(true)
        Log.warnWithException(logger, e, "one-shot task {} failed {}", name, e)
        ErrorReporter.getInstance().reportError("org.digma.scheduler.blockingOneShotTask", e, mapOf("task.name" to name))
        return false
    }

}


/**
 * executes a task with result , waiting for the task to finish within timeout and return its result.
 * rethrows exceptions including InterruptedException
 */
@Throws(InterruptedException::class, Exception::class)
fun <T> blockingOneShotTaskWithResult(name: String, timeoutMillis: Long, block: () -> T): T {

    Log.log(logger::trace, "registering blockingOneShotTaskWithResult {}, timeoutMillis:{}", name, timeoutMillis)

    val future = try {

        scheduler.submit(Callable {
            val stopWatch = StopWatch.createStarted()
            try {
                Log.logWithThreadName(logger::trace, "executing one-shot task with result {}", name)
                val result = block.invoke()

                return@Callable result
            } finally {
                stopWatch.stop()
                Log.logWithThreadName(logger::trace, "one-shot task with result {} completed in {} millis", name, stopWatch.time)
                scheduledTasksPerformanceMonitor.reportPerformance(name, stopWatch.time)
            }
        })

    } catch (e: Throwable) {
        Log.warnWithException(logger, e, "could not submit one-shot task with result {}", name)
        ErrorReporter.getInstance().reportError("org.digma.scheduler.blockingOneShotTaskWithResult", e, mapOf("task.name" to name))
        throw e
    }

    try {
        return future.get(timeoutMillis, TimeUnit.MILLISECONDS)
    } catch (e: InterruptedException) {
        Log.logWithThreadName(logger::trace, "one-shot task with result {} interrupted", name)
        //don't log InterruptedException to posthog, it doesn't help us
        throw e
    } catch (e: Throwable) {
        future.cancel(true)
        Log.warnWithException(logger, e, "one-shot task with result {} failed {}", name, e)
        ErrorReporter.getInstance().reportError("org.digma.scheduler.blockingOneShotTaskWithResult", e, mapOf("task.name" to name))
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
                    "task.name" to taskName,
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
    private val currentlyExecutingThreads = Collections.synchronizedMap(mutableMapOf<Runnable, Thread>())
    private val interrupting = AtomicBoolean(false)
    private val checkCapacityEveryRound = 3

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


    override fun beforeExecute(t: Thread?, r: Runnable?) {
        if (interrupting.get()) {
            Log.log(logger::trace, "interrupting thread {}", t?.name)
            t?.interrupt()
        } else {
            if (t != null && r != null) {
                currentlyExecutingThreads[r] = t
            }
        }
    }

    override fun afterExecute(r: Runnable?, t: Throwable?) {
        if (r != null) {
            currentlyExecutingThreads.remove(r)
        }
    }

    fun manage() {
        managementRound++

        val poolSize = scheduler.corePoolSize
        val maxPoolSize = scheduler.maximumPoolSize
        val activeCount = scheduler.activeCount
        val taskQueueSize = scheduler.queue.size

        Log.logWithThreadName(
            logger::trace,
            "management snapshot: poolSize:{} , activeCount:{} , task queue:{} , registered recurring task(includes canceled):{}",
            poolSize,
            activeCount,
            taskQueueSize,
            registeredRecurringTasks
        )

        if (activeCount == poolSize) exhaustedCount.incrementAndGet()

        //check every 5 rounds, if was always exhausted increase pool size
        if (managementRound % checkCapacityEveryRound == 0) {
            val exhausted = exhaustedCount.get()
            exhaustedCount.set(0)
            if (exhausted >= checkCapacityEveryRound) {
                Log.log(logger::trace, "management: had full capacity in the past 5 rounds")
                if (poolSize < maxPoolSize) {
                    val currentSize = corePoolSize
                    corePoolSize = min(maxPoolSize, poolSize + 1)
                    Log.logWithThreadName(logger::trace, "management: pool size increased from {} to {}", currentSize, corePoolSize)
                } else {
                    Log.logWithThreadName(logger::trace, "management: can't increase pool size because at max")
                }
            }
        }

        //send statistics every 2 hours
        if (managementRound % 60 == 0) {
            findActiveProject()?.let { project ->
                ActivityMonitor.getInstance(project).registerSchedulerStatistics(
                    mapOf(
                        "core.pool.size" to scheduler.corePoolSize,
                        "max.pool.size" to scheduler.maximumPoolSize,
                        "task.queue.size" to scheduler.queue.size,
                        "all.registered.recurring (including canceled)" to scheduler.registeredRecurringTasks,
                        "max.pool.size.allowed" to SCHEDULER_MAX_SIZE,
                        "max.queue.size.allowed" to SCHEDULER_MAX_QUEUE_SIZE_ALLOWED
                    )
                )
            }
        }
    }

    fun interruptAll() {
        try {
            interrupting.set(true)
            currentlyExecutingThreads.forEach {
                try {
                    Log.log(logger::trace, "interrupting thread {}", it.value.name)
                    it.value.interrupt()
                } catch (e: Throwable) {
                    Log.warnWithException(logger, e, "can't interrupt thread {}", it.value.name)
                }
            }
        } finally {
            interrupting.set(false)
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
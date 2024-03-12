package org.digma.intellij.plugin.progress

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.apache.commons.lang3.time.StopWatch
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.errorreporting.SEVERITY_LOW_NO_FIX
import org.digma.intellij.plugin.errorreporting.SEVERITY_MEDIUM_TRY_FIX
import org.digma.intellij.plugin.errorreporting.SEVERITY_PROP_NAME
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Runs on new background thread with visible progress in status bar.
 * @see org.digma.intellij.plugin.progress.RetryableTask.Invisible
 */
open class RetryableTask(
    // project can not be changed.
    val project: Project,
    //title may be changed between retries in beforeRetryTask.
    var title: String,
    // canBeCanceled can not be changed.
    val canBeCanceled: Boolean = false,
    //workTask will run on the main process thread and may be replaced in beforeRetryTask.
    var workTask: Consumer<ProgressIndicator>,
    //beforeRetryTask will run on a background thread after process canceled and before the next retry,
    // may be used to change the task properties for the next retry.
    var beforeRetryTask: Consumer<RetryableTask>? = null,
    //shouldRetryTask will run on a background thread if process was canceled and is meant for quick decision if to continue retrying.
    //may be replaced in beforeRetryTask.
    var shouldRetryTask: Supplier<Boolean>? = null,
    //onErrorTask will run on a background thread and is meant for reporting only if necessary.
    //may be replaced in beforeRetryTask.
    var onErrorTask: Consumer<Throwable>? = null,
    //onPCETask will run on a background thread if the process was canceled and exhausted or stopped.
    //may be replaced in beforeRetryTask.
    var onPCETask: Consumer<ProcessCanceledException>? = null,
    //onFinish will run when all on success or after exhausted ob background thread.
    // can check task properties to decide if exhausted or stopped or success.
    //may be replaced in beforeRetryTask.
    var onFinish: Consumer<RetryableTask>? = null,
    var maxRetries: Int = 10,
    var delayBetweenRetriesMillis: Long = 2000L,
) {

    private var retry = 0 // keep retry hidden, so it can't be changed dramatically, only incremented by one
    private var stoppedBeforeExhausted = false
    private var success = false
    var processCanceledException: ProcessCanceledException? = null
    var error: Throwable? = null

    open fun runInBackground() {
        runBackgroundTaskInProgressWithRetry(this)
    }

    fun getRetryCount(): Int {
        return retry
    }

    fun incrementRetry() {
        retry++
    }

    fun isExhausted(): Boolean {
        return getRetryCount() >= maxRetries
    }

    fun stopRetrying() {
        stoppedBeforeExhausted = true
    }

    fun isStoppedBeforeExhausted(): Boolean {
        return stoppedBeforeExhausted
    }

    fun setCompletedSuccessfully() {
        success = true
    }

    fun isCompletedSuccessfully(): Boolean {
        return success
    }

    fun shouldContinueRetry(): Boolean {
        return !isExhausted() && !isStoppedBeforeExhausted()
    }


    /**
     * Runs on new background thread or calling thread with NO visible progress in status bar
     */
    class Invisible(
        project: Project,
        title: String,
        workTask: Consumer<ProgressIndicator>,
        beforeRetryTask: Consumer<RetryableTask>? = null,
        shouldRetryTask: Supplier<Boolean>? = null,
        onErrorTask: Consumer<Throwable>? = null,
        onPCETask: Consumer<ProcessCanceledException>? = null,
        onFinish: Consumer<RetryableTask>? = null,
        maxRetries: Int = 10,
        delayBetweenRetriesMillis: Long = 2000L,
    ) : RetryableTask(
        project,
        title,
        false,
        workTask,
        beforeRetryTask,
        shouldRetryTask,
        onErrorTask,
        onPCETask,
        onFinish,
        maxRetries,
        delayBetweenRetriesMillis
    ) {
        var reuseCurrentThread = true

        override fun runInBackground() {
            runInvisibleBackgroundTaskInProgressWithRetry(this)
        }
    }

}


private class Example {


    /**
     * example how to create a RetryableTask
     */
    fun myExampleRetryableTask(project: Project) {


        val stopWatch = StopWatch.createStarted()

        var context: ProcessContext? = null
        val workTask = Consumer<ProgressIndicator> {
            val myContext = ProcessContext(it)
            context = myContext

            //can prepare data here if necessary
            // This is the main processing point, call any method here
        }

        val beforeRetryTask = Consumer<RetryableTask> { task ->
            //this is a hook before every retry to change anything on the task,
            // may even change the work task or anything else.

            //increase the delay on every retry
            task.delayBetweenRetriesMillis += 5000L
        }

        val shouldRetryTask = Supplier<Boolean> {
            //a hook to stop retrying, for example stop retrying if the project is disposed
            //isProjectValid(project)
            true
        }

        val onErrorTask = Consumer<Throwable> {
            ErrorReporter.getInstance().reportError(
                "JvmSpanNavigationProvider.buildSpanNavigationUnderProgress.onError", it, mapOf(
                    SEVERITY_PROP_NAME to SEVERITY_MEDIUM_TRY_FIX
                )
            )
        }

        val onPCETask = Consumer<ProcessCanceledException> {
            ErrorReporter.getInstance().reportError(
                "JvmSpanNavigationProvider.buildSpanNavigationUnderProgress.onPCE", it, mapOf(
                    SEVERITY_PROP_NAME to SEVERITY_LOW_NO_FIX
                )
            )
        }

        val onFinish = Consumer<RetryableTask> { task ->

            val hadProgressErrors = task.error != null
            val hadPCE = task.processCanceledException != null
            val success = task.isCompletedSuccessfully()

            //can log success or failure
//            if (success) {
//                Log.log(logger::info, "buildSpanNavigation completed successfully")
//            } else {
//                if (hadProgressErrors) {
//                    Log.log(logger::info, "buildSpanNavigation completed with errors")
//                } else if (hadPCE && task.isExhausted()) {
//                    Log.log(logger::info, "buildSpanNavigation process retry exhausted")
//                } else if (hadPCE && task.isStoppedBeforeExhausted()) {
//                    Log.log(logger::info, "buildSpanNavigation completed before exhausted")
//                } else {
//                    Log.log(logger::info, "buildSpanNavigation completed abnormally")
//                }
//            }

            val time = stopWatch.getTime(TimeUnit.MILLISECONDS)
            val hadErrors = context?.hasErrors() ?: false
            //can report here the result of the task
        }


        val task = RetryableTask(
            project = project,
            title = "My task name",
            workTask = workTask,
            beforeRetryTask = beforeRetryTask,
            shouldRetryTask = shouldRetryTask,
            onErrorTask = onErrorTask,
            onFinish = onFinish,
            onPCETask = onPCETask,
            maxRetries = 10,
            delayBetweenRetriesMillis = 2000L
        )

        task.runInBackground()
    }


    /**
     * example how to create a RetryableTask.Invisible
     */
    fun myExampleRetryableTaskInvisible(project: Project) {


        val stopWatch = StopWatch.createStarted()

        var context: ProcessContext? = null
        val workTask = Consumer<ProgressIndicator> {
            val myContext = ProcessContext(it)
            context = myContext

            //can prepare data here if necessary
            // This is the main processing point, call any method here
        }

        val beforeRetryTask = Consumer<RetryableTask> { task ->
            //this is a hook before every retry to change anything on the task,
            // may even change the work task or anything else.

            //increase the delay on every retry
            task.delayBetweenRetriesMillis += 5000L
        }

        val shouldRetryTask = Supplier<Boolean> {
            //a hook to stop retrying, for example stop retrying if the project is disposed
            //isProjectValid(project)
            true
        }

        val onErrorTask = Consumer<Throwable> {
            ErrorReporter.getInstance().reportError(
                "JvmSpanNavigationProvider.buildSpanNavigationUnderProgress.onError", it, mapOf(
                    SEVERITY_PROP_NAME to SEVERITY_MEDIUM_TRY_FIX
                )
            )
        }

        val onPCETask = Consumer<ProcessCanceledException> {
            ErrorReporter.getInstance().reportError(
                "JvmSpanNavigationProvider.buildSpanNavigationUnderProgress.onPCE", it, mapOf(
                    SEVERITY_PROP_NAME to SEVERITY_LOW_NO_FIX
                )
            )
        }

        val onFinish = Consumer<RetryableTask> { task ->

            val hadProgressErrors = task.error != null
            val hadPCE = task.processCanceledException != null
            val success = task.isCompletedSuccessfully()

            //can log success or failure
//            if (success) {
//                Log.log(logger::info, "buildSpanNavigation completed successfully")
//            } else {
//                if (hadProgressErrors) {
//                    Log.log(logger::info, "buildSpanNavigation completed with errors")
//                } else if (hadPCE && task.isExhausted()) {
//                    Log.log(logger::info, "buildSpanNavigation process retry exhausted")
//                } else if (hadPCE && task.isStoppedBeforeExhausted()) {
//                    Log.log(logger::info, "buildSpanNavigation completed before exhausted")
//                } else {
//                    Log.log(logger::info, "buildSpanNavigation completed abnormally")
//                }
//            }

            val time = stopWatch.getTime(TimeUnit.MILLISECONDS)
            val hadErrors = context?.hasErrors() ?: false
            //can report here the result of the task
        }


        val task = RetryableTask.Invisible(
            project = project,
            title = "My task name", //title is ignored in RetryableTask.Invisible
            workTask = workTask,
            beforeRetryTask = beforeRetryTask,
            shouldRetryTask = shouldRetryTask,
            onErrorTask = onErrorTask,
            onFinish = onFinish,
            onPCETask = onPCETask,
            maxRetries = 10,
            delayBetweenRetriesMillis = 2000L
        )

        task.reuseCurrentThread = true
        task.runInBackground()
    }

}
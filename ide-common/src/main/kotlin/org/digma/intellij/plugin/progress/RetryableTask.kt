package org.digma.intellij.plugin.progress

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Runs on new background thread with visible progress in status bar
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
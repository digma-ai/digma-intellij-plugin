package org.digma.intellij.plugin.progress

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task

/**
 * This task will retry on ProcessCanceledException.
 * other exceptions will not be retried, to retry on other exceptions use the regular retry utilities on different code blocks.
 */
internal class RetryableBackgroundTask(val task: RetryableTask) :
    Task.Backgroundable(task.project, task.title, task.canBeCanceled) {

    private var canceled = false

    override fun run(indicator: ProgressIndicator) {
        try {
            task.processCanceledException = null
            task.error = null
            task.workTask.accept(indicator)
            task.setCompletedSuccessfully()
        } catch (e: ProcessCanceledException) {
            task.processCanceledException = e
            throw e
        }
    }

    //runs on EDT
    override fun onCancel() {
        canceled = true
    }

    //runs on EDT
    override fun onThrowable(error: Throwable) {
        task.error = error
        org.digma.intellij.plugin.common.Backgroundable.executeOnPooledThread {
            task.onErrorTask?.accept(error)
        }
    }

    //runs on EDT
    override fun onFinished() {

        org.digma.intellij.plugin.common.Backgroundable.executeOnPooledThread {

            if (canceled) {
                val shouldRetry = task.shouldRetryTask?.get() ?: true
                if (!shouldRetry) {
                    task.stopRetrying()
                }

                if (!task.shouldContinueRetry()) {
                    task.processCanceledException?.let {
                        task.onPCETask?.accept(it)
                    }
                    task.onFinish?.accept(task)
                } else {
                    retry()
                }
            } else {
                task.onFinish?.accept(task)
            }
        }
    }

    private fun retry() {

        task.beforeRetryTask?.accept(task)
        task.incrementRetry()
        try {
            Thread.sleep(task.delayBetweenRetriesMillis)
        } catch (_: InterruptedException) {
        }

        task.runInBackground()
    }
}
package org.digma.intellij.plugin.progress

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager

class InvisibleRetryableTask(val task: RetryableTask) : Runnable {


    //will run on current thread and retry until exhausted
    override fun run() {
        var canceled = false
        do {
            canceled = false
            val indicator = EmptyProgressIndicator()

            try {

                ProgressManager.getInstance().runProcess({
                    task.workTask.accept(indicator)
                }, indicator)

                task.setCompletedSuccessfully()
            } catch (
                @Suppress("IncorrectProcessCanceledExceptionHandling")
                e: ProcessCanceledException,
            ) {
                //don't want to throw ProcessCanceledException, want to retry
                canceled = true
                task.processCanceledException = e
            } catch (error: Throwable) {
                task.error = error
                task.onErrorTask?.accept(error)
            }


            if (!task.isCompletedSuccessfully()) {

                val shouldRetry = task.shouldRetryTask?.get() ?: true
                if (!shouldRetry) {
                    task.stopRetrying()
                }

                task.incrementRetry()

                //means going to finish so call the onPCETask
                if (!task.shouldContinueRetry() && canceled) {
                    task.processCanceledException?.let {
                        task.onPCETask?.accept(it)
                    }
                }

                if (task.shouldContinueRetry()) {
                    //going to retry
                    task.beforeRetryTask?.accept(task)
                    try {
                        Thread.sleep(task.delayBetweenRetriesMillis)
                    } catch (_: InterruptedException) {
                    }
                }
            }

        } while (!task.isCompletedSuccessfully() && task.shouldContinueRetry())


        task.onFinish?.accept(task)

    }

}
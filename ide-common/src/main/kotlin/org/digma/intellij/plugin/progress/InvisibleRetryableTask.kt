package org.digma.intellij.plugin.progress

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import org.digma.intellij.plugin.common.isProjectValid

/**
 * runs RetryableTask.Invisible under progress.
 * the calling thread should not already be running under progress, if it does it will break the behaviour
 * of the calling progress.
 */
internal class InvisibleRetryableTask(val task: RetryableTask.Invisible) : Runnable {


    override fun run() {

        if (!isProjectValid(task.project)) {
            return
        }

        //must not be under progress already
        assertNotUnderProgress()

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
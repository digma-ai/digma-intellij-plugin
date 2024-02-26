package org.digma.intellij.plugin.progress

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import org.digma.intellij.plugin.common.Backgroundable


fun runBackgroundTaskInProgressWithRetry(task: RetryableTask) {
    val backgroundTask = RetryableBackgroundTask(task)
    ProgressManager.getInstance().run(backgroundTask)
}


fun runInvisibleBackgroundTaskInProgressWithRetry(task: RetryableTask.Invisible) {

    val invisibleRetryableTask = InvisibleRetryableTask(task)

    if (task.reuseCurrentThread) {
        //even if reuseCurrentThread is true , still make sure it's a background thread. these tasks
        // are not meant to run on EDT.
        Backgroundable.ensurePooledThread {
            invisibleRetryableTask.run()
        }
    } else {
        Backgroundable.executeOnPooledThread {
            invisibleRetryableTask.run()
        }
    }
}


fun assertUnderProgress() {
    if (ProgressManager.getGlobalProgressIndicator() == null) {
        //we usually don't write error, but this error must be caught during development
        // to warn us of wrong use of retryable task. it will pop up a red balloon.
        Logger.getInstance("ProgressUtilsKt").error("must run under progress")
        throw RuntimeException("must run under progress")
    }
}

fun assertNotUnderProgress() {
    if (ProgressManager.getGlobalProgressIndicator() != null) {
        //we usually don't write error, but this error must be caught during development
        // to warn us of wrong use of retryable task. it will pop up a red balloon.
        Logger.getInstance("ProgressUtilsKt").error("must NOT be called under progress")
        throw RuntimeException("must NOT be called under progress")
    }
}
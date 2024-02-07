package org.digma.intellij.plugin.progress

import com.intellij.openapi.progress.ProgressManager
import org.digma.intellij.plugin.common.Backgroundable


fun runBackgroundTaskInProgressWithRetry(task: RetryableTask) {
    val backgroundTask = RetryableBackgroundTask(task)
    ProgressManager.getInstance().run(backgroundTask)
}


fun runInvisibleBackgroundTaskInProgressWithRetry(task: RetryableTask.Invisible) {

    val invisibleRetryableTask = InvisibleRetryableTask(task)

    if (task.reuseCurrentThread) {
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
        throw RuntimeException("must run under progress")
    }
}
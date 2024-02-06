package org.digma.intellij.plugin.progress

import com.intellij.openapi.progress.ProgressManager


fun runBackgroundTaskInProgressWithRetry(task: RetryableTask) {
    val backgroundTask = RetryableBackgroundTask(task)
    ProgressManager.getInstance().run(backgroundTask)
}
package org.digma.intellij.plugin.common

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import org.digma.intellij.plugin.log.Log
import java.util.concurrent.locks.ReentrantLock

@Suppress("DialogTitle")
class AsynchronousBackgroundTask(private val lock: ReentrantLock, private val task: Runnable) : Task.Backgroundable(null, "Loading recent activity data") {
    private val logger: Logger = Logger.getInstance(AsynchronousBackgroundTask::class.java)
    override fun run(indicator: ProgressIndicator) {
        if (lock.tryLock()) {
            try {
                task.run()
            } finally {
                lock.unlock()
            }
        } else {
            // Previous task is still in progress, skip this task
            Log.log(logger::warn, "New task was skip because previous task is still in progress.")
        }
    }
}

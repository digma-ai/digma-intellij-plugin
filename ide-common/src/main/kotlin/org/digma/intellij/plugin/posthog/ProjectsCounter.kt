package org.digma.intellij.plugin.posthog

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectCloseListener
import com.intellij.openapi.startup.ProjectActivity
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.session.SessionMetadataProperties
import org.digma.intellij.plugin.session.getPluginLoadedKey
import java.util.concurrent.atomic.AtomicInteger

object ProjectsCounter {
    val counter = AtomicInteger(0)

}

@Suppress("UnstableApiUsage")
class CountingProjectCloseListener : ProjectCloseListener {

    override fun projectClosing(project: Project) {
        try {
            Log.log(Logger.getInstance(ProjectsCounter::class.java)::trace, "got project closed {}", project.name)
            ActivityMonitor.getInstance(project).registerProjectClosed(ProjectsCounter.counter.decrementAndGet())
            SessionMetadataProperties.getInstance().delete(getPluginLoadedKey(project))
        } catch (e: Throwable) {
            Log.warnWithException(Logger.getInstance(ProjectsCounter::class.java), e, "error in projectClosing")
        }
    }
}

class CountingProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        try {
            Log.log(Logger.getInstance(ProjectsCounter::class.java)::trace, "got project opened {}", project.name)
            ActivityMonitor.getInstance(project).registerProjectOpened(ProjectsCounter.counter.incrementAndGet())
            SessionMetadataProperties.getInstance().put(getPluginLoadedKey(project), true)
        } catch (e: Throwable) {
            Log.warnWithException(Logger.getInstance(ProjectsCounter::class.java), e, "error in ProjectActivity")
        }
    }

}


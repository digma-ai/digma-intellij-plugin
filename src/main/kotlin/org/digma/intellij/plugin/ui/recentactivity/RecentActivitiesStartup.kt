package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.collaboration.async.DisposingScope
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log

class RecentActivitiesStartup : StartupActivity {

    private val logger = Logger.getInstance(this::class.java)

    override fun runActivity(project: Project) {

        Log.log(logger::info, "RecentActivitiesStartup called")

        @Suppress("UnstableApiUsage")
        DisposingScope(project.service<RecentActivityUpdater>()).launch {

            while (isActive) {
                delay(10000)
                try {
                    if (project.service<BackendConnectionMonitor>().isConnectionOk()) {
                        Log.log(logger::trace, "calling updateLatestActivities")
                        project.service<RecentActivityUpdater>().updateLatestActivities()
                    } else {
                        Log.log(logger::trace, "no connection, not calling updateLatestActivities")
                    }
                } catch (e: CancellationException) {
                    Log.log(logger::trace, project, "recent activity timer job canceled")
                    break
                } catch (e: Exception) {
                    Log.warnWithException(logger, e, "Exception updating RecentActivities")
                    service<ErrorReporter>().reportError(project, "RecentActivityService.updateRecentActivitiesTimer", e)
                }
            }
        }
    }
}
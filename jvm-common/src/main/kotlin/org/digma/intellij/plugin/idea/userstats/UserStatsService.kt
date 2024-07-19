package org.digma.intellij.plugin.idea.userstats

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import org.digma.intellij.plugin.startup.DigmaProjectActivity
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Service(Service.Level.PROJECT)
class UserStatsService(private val project: Project) : DisposableAdaptor {

    val logger = Logger.getInstance(UserStatsService::class.java)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): UserStatsService {
            return project.getService(UserStatsService::class.java)
        }
    }

    init {

        disposingPeriodicTask("UserStatsService.periodicAction", 1.minutes.inWholeMilliseconds, 2.hours.inWholeMilliseconds) {
            try {
                periodicAction()
            } catch (e: Exception) {
                Log.warnWithException(logger, e, "Exception in periodicAction")
                ErrorReporter.getInstance().reportError(project, "UserStatsService.periodicAction", e)
            }
        }
    }

    private fun periodicAction() {
        if (project.isDisposed) return
        val userUsageStats = AnalyticsService.getInstance(project).userUsageStats
        ActivityMonitor.getInstance(project).reportUserUsageStats(userUsageStats)
    }
}

class UserStatsServiceStarter : DigmaProjectActivity() {
    override fun executeProjectStartup(project: Project) {
        UserStatsService.getInstance(project)
    }
}

package org.digma.intellij.plugin.idea.userstats

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.util.concurrent.TimeUnit

class UserStatsService(private val project: Project) : Disposable {

    companion object {
        private val logger = Logger.getInstance(UserStatsService::class.java)

        @JvmStatic
        fun getInstance(project: Project): UserStatsService {
            return project.getService(UserStatsService::class.java)
        }
    }

    private var firstDelay: Boolean = true

    init {

        // DBG
//        println("DBG: UserStatsService init")

        @Suppress("UnstableApiUsage")
        disposingScope().launch {
            while (isActive) {
                try {
                    // DBG
//                    println("DBG:UserStatsService pre delay")
                    doDelay()
                    if (isActive) {
                        periodicAction()
                    }
                } catch (e: Exception) {
                    Log.warnWithException(logger, e, "Exception in periodicAction")
                    ErrorReporter.getInstance().reportError(project, "UserStatsService.periodicAction", e)
                }
            }
        }
    }

    private suspend fun doDelay() {
        if (firstDelay) {
            firstDelay = false
            delay(TimeUnit.MINUTES.toMillis(1))
        } else {
            delay(TimeUnit.HOURS.toMillis(2)) // prod
//          delay(TimeUnit.MINUTES.toMillis(1))       // debug
        }
    }

    private fun periodicAction() {
        if (project.isDisposed) return

        val userUsageStats = AnalyticsService.getInstance(project).userUsageStats
        ActivityMonitor.getInstance(project).reportUserUsageStats(userUsageStats)
    }

    override fun dispose() {
        //nothing to do , used as disposable parent
    }

}

class UserStatsServiceStarter : StartupActivity {
    override fun runActivity(project: Project) {
        // its enough just to have reference to the service, and it will get initialized
        val service = UserStatsService.getInstance(project)
    }
}

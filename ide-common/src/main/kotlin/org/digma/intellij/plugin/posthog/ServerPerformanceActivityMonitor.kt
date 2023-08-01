package org.digma.intellij.plugin.posthog

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsProviderException
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import javax.swing.Timer

class ServerPerformanceActivityMonitor(private val project: Project) : AnalyticsServiceConnectionEvent, Disposable {

    companion object {
        private val LOGGER = Logger.getInstance(ServerPerformanceActivityMonitor::class.java)
        private val MAX_FAILURES = 10
        @JvmStatic
        fun loadInstance(project: Project) {
            project.getService(ServerPerformanceActivityMonitor::class.java)
        }
    }

    private val timer: Timer
    private var failures: Int = 0

    init {
        project.messageBus.connect().subscribe(
            AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC,
            this
        )
        timer = Timer(10 * 1000) {
            checkForServerPerformance()
        }
        timer.start()
    }

    override fun connectionLost() {
        timer.stop()
    }

    override fun connectionGained() {
        if (!PersistenceService.getInstance().state.firstTimePerformanceMetrics) {
            failures = 0
            timer.start()
        }
    }

    private fun checkForServerPerformance() {
        Backgroundable.ensureBackground(project, "Fetching server performance metrics") {
            try {
                // [!] The "performance" endpoint must NOT be called via the proxy
                //     so failing requests won't mark the connection as "lost",
                //     due to older server versions which does not contain this
                //     endpoint yet
                val result = AnalyticsService.getInstance(project).analyticsProvider.performanceMetrics
                if (!result?.metrics.isNullOrEmpty() &&
                    !PersistenceService.getInstance().state.firstTimePerformanceMetrics
                ) {
                    ActivityMonitor.getInstance(project).registerPerformanceMetrics(result)
                    PersistenceService.getInstance().state.firstTimePerformanceMetrics = true
                    timer.stop()
                }
            } catch (e: Exception) {
                if(e is AnalyticsProviderException && e.responseCode == 404) {
                    timer.stop()
                }
                Log.log(LOGGER::warn, "Failed to get+register server performance metrics (try ${failures}/${MAX_FAILURES}): {}", e.message)
                failures++
            }
            finally {
                if(failures >= MAX_FAILURES)
                    timer.stop()
            }
        }
    }

    override fun dispose() {
        timer.stop()
    }
}
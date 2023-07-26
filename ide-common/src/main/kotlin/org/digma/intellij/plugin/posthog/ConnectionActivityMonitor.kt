package org.digma.intellij.plugin.posthog

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.log.Log

class ConnectionActivityMonitor(private val project: Project) : AnalyticsServiceConnectionEvent {

    companion object {
        private val LOGGER = Logger.getInstance(ConnectionActivityMonitor::class.java)

        @JvmStatic
        fun loadInstance(project: Project) {
            project.getService(ConnectionActivityMonitor::class.java)
        }
    }

    init {
        project.messageBus.connect().subscribe(
            AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC,
            this
        )
        asyncFetchAndRegisterServerVersion()
    }

    override fun connectionLost() {

    }

    override fun connectionGained() {
        asyncFetchAndRegisterServerVersion()
    }

    private fun asyncFetchAndRegisterServerVersion(){
        Log.log(LOGGER::trace, "Fetching server about info")
        Backgroundable.runInNewBackgroundThread(project, "Fetching server about info") {
            try {
                // [!] The "about" endpoint must NOT be called via the proxy
                //     so failing requests won't mark the connection as "lost",
                //     due to older server versions which does not contain this
                //     endpoint yet
                val about = AnalyticsService.getInstance(project).analyticsProvider.about
                ActivityMonitor.getInstance(project).registerServerInfo(about)
            } catch (e: Exception) {
                Log.warnWithException(LOGGER, e, "Failed to get+register server version: {}", e.message);
            }
        }
    }
}
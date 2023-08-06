package org.digma.intellij.plugin.posthog

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.log.Log

class ConnectionActivityMonitor(private val project: Project) : AnalyticsServiceConnectionEvent {

    companion object {
        private val logger = Logger.getInstance(ConnectionActivityMonitor::class.java)

        @JvmStatic
        fun loadInstance(project: Project) {
            Log.test(logger,"Getting instance of ${ConnectionActivityMonitor::class.simpleName}")
            project.getService(ConnectionActivityMonitor::class.java)
            Log.test(logger,"Returning ${ConnectionActivityMonitor::class.simpleName}")
        }
    }


    init {
        Log.test(logger,"Initializing ${ConnectionActivityMonitor::class.simpleName}")
        project.messageBus.connect().subscribe(
            AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC,
            this
        )
//        Log.test(logger,"Skipping call to asyncFetchAndRegisterServerVersion")
        asyncFetchAndRegisterServerVersion()
        Log.test(logger,"Finished ${ConnectionActivityMonitor::class.simpleName} initialization")
    }

    override fun connectionLost() {

    }

    override fun connectionGained() {
//        Log.test(logger,"Skipping call to asyncFetchAndRegisterServerVersion")
        asyncFetchAndRegisterServerVersion()
    }

    private fun asyncFetchAndRegisterServerVersion(){
        Backgroundable.ensureBackground(project, "Fetching server about info") {
            try {
                // [!] The "about" endpoint must NOT be called via the proxy
                //     so failing requests won't mark the connection as "lost",
                //     due to older server versions which does not contain this
                //     endpoint yet
                Log.test(logger, "Calling AnalyticsService.getInstance(project).analyticsProvider.about");
                val about = AnalyticsService.getInstance(project).analyticsProvider.about
                Log.test(logger, "Calling ActivityMonitor.getInstance(project).registerServerInfo(about)");
                ActivityMonitor.getInstance(project).registerServerInfo(about)
            } catch (e: Exception) {
                Log.log(logger::warn, "Failed to get+register server version: {}", e.message);
            }
            Log.test(logger, "Done fetching server about info");
        }
    } 
}
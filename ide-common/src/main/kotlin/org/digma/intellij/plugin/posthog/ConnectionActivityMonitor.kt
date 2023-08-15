package org.digma.intellij.plugin.posthog

import com.intellij.collaboration.async.DisposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.log.Log

@Service(Service.Level.PROJECT)
class ConnectionActivityMonitor(private val project: Project) : AnalyticsServiceConnectionEvent,Disposable {

    private var appVersion: String? = null

    companion object {
        private val LOGGER = Logger.getInstance(ConnectionActivityMonitor::class.java)

        @JvmStatic
        fun loadInstance(project: Project) {
            project.getService(ConnectionActivityMonitor::class.java)
        }
    }


    init {
        Log.test(LOGGER::info,"Initializing ${ConnectionActivityMonitor::class.simpleName}")
        project.messageBus.connect().subscribe(
            AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC,
            this
        )
        asyncFetchAndRegisterServerVersion()
        Log.test(LOGGER::info,"Finished ${ConnectionActivityMonitor::class.simpleName} initialization")
    }

    override fun connectionLost() {
        //nothing to do
    }

    override fun connectionGained() {
        asyncFetchAndRegisterServerVersion()
    }

    private fun asyncFetchAndRegisterServerVersion(){
        Log.log(LOGGER::trace, "Fetching server about info")

        @Suppress("UnstableApiUsage")
        DisposingScope(this).launch {
            try {
                val about = AnalyticsService.getInstance(project).about
                if (appVersion != about.applicationVersion) {
                    appVersion = about.applicationVersion
                    ActivityMonitor.getInstance(project).registerServerInfo(about)
                }
            } catch (e: Exception) {
//                Log.warnWithException(LOGGER, e, "Failed to get+register server version: {}", e.message)
                Log.test(LOGGER::warn, "Failed to get+register server version: {}", e.message)
            }
            Log.test(LOGGER::info, "Done fetching server about info");
        }
    }

    override fun dispose() {
        //nothing to do, used as parent disposable
    }
}
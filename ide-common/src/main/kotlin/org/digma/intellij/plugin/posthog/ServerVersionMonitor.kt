package org.digma.intellij.plugin.posthog

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.log.Log

@Service(Service.Level.PROJECT)
class ServerVersionMonitor(private val project: Project) : AnalyticsServiceConnectionEvent, Disposable {

    private var appVersion: String? = null

    companion object {
        private val LOGGER = Logger.getInstance(ServerVersionMonitor::class.java)

        @JvmStatic
        fun getInstance(project: Project): ServerVersionMonitor {
            return project.service<ServerVersionMonitor>()
        }
    }

    init {
        project.messageBus.connect().subscribe(
            AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC,
            this
        )
        asyncFetchAndRegisterServerVersion()
    }


    fun getServerVersion(): String {
        return appVersion.toString()
    }


    override fun connectionLost() {
        //nothing to do
    }

    override fun connectionGained() {
        asyncFetchAndRegisterServerVersion()
    }

    private fun asyncFetchAndRegisterServerVersion() {
        Log.log(LOGGER::trace, "Fetching server about info")

        @Suppress("UnstableApiUsage")
        disposingScope().launch {
            try {
                val about = AnalyticsService.getInstance(project).about
                if (appVersion != about.applicationVersion) {
                    appVersion = about.applicationVersion
                    ActivityMonitor.getInstance(project).registerServerInfo(about)
                }
            } catch (e: AnalyticsServiceException) {
                Log.debugWithException(LOGGER, e, "Failed to get+register server version: {}", e.message)
            } catch (e: Exception) {
                Log.warnWithException(LOGGER, e, "Failed to get+register server version: {}", e.message)
            }
        }
    }

    override fun dispose() {
        //nothing to do, used as parent disposable
    }
}
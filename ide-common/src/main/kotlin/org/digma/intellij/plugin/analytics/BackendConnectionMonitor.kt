package org.digma.intellij.plugin.analytics

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import org.digma.intellij.plugin.log.Log;

/**
 * This service keeps track of the analytics service connection status. it is used to decide when to show the
 * no connection message in the plugin window.
 * see also class NoConnectionPanel
 */
@Service(Service.Level.PROJECT)
class BackendConnectionMonitor(val project: Project) : Disposable, AnalyticsServiceConnectionEvent {

    companion object {
        private val logger = Logger.getInstance(BackendConnectionMonitor::class.java)
        @JvmStatic
        fun getInstance(project: Project): BackendConnectionMonitor {
            return project.getService(BackendConnectionMonitor::class.java)
        }
    }

    private var hasConnectionError = false

    private val analyticsConnectionEventsConnection: MessageBusConnection = project.messageBus.connect()

    init {
        analyticsConnectionEventsConnection.subscribe(
            AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC,
            this
        )
    }

    override fun dispose() {
        Log.test(logger::info,"analyticsConnectionEventsConnection.dispose()")
        analyticsConnectionEventsConnection.dispose()
        hasConnectionError = false
    }

    fun isConnectionError(): Boolean {
        return false
//        return hasConnectionError // this is the real implementation
        
    }

    fun isConnectionOk(): Boolean {
        return true
//        return !hasConnectionError  // this is the real implementation
    }

    private fun connectionError() {
        hasConnectionError = true
    }

    private fun connectionOk() {
        hasConnectionError = false
    }

    override fun connectionLost() {
        connectionError()
    }

    override fun connectionGained() {
        connectionOk()
    }

}
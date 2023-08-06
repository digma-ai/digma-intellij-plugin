package org.digma.intellij.plugin.analytics

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import org.digma.intellij.plugin.log.Log;

/**
 * This service keeps track of the analytics service connection status. it is used to decide when to show the
 * no connection message in the plugin window.
 * see also class NoConnectionPanel
 */
class BackendConnectionMonitor(val project: Project) : Disposable, AnalyticsServiceConnectionEvent {

    companion object {
        private val logger = Logger.getInstance(BackendConnectionMonitor::class.java)
        @JvmStatic
        fun getInstance(project: Project): BackendConnectionMonitor {
            Log.test(logger,"Getting instance of ${BackendConnectionMonitor::class.simpleName}")
            val service = project.getService(BackendConnectionMonitor::class.java)
            Log.test(logger, "Returning ${BackendConnectionMonitor::class.simpleName}")
            return service;
        }
    }

    private var hasConnectionError = false

    private val analyticsConnectionEventsConnection: MessageBusConnection = project.messageBus.connect()

    init {
        Log.test(logger,"Initializing ${BackendConnectionMonitor::class.simpleName}")
        analyticsConnectionEventsConnection.subscribe(
            AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC,
            this
        )
        Log.test(logger,"Finished ${BackendConnectionMonitor::class.simpleName} initialization")
    }

    override fun dispose() {
        Log.test(logger,"Disposing")
        analyticsConnectionEventsConnection.dispose()
        hasConnectionError = false
        Log.test(logger,"Finished disposing")
    }

    fun isConnectionError(): Boolean {
        Log.test(logger,"isConnectionError")
        return false
    }

    fun isConnectionOk(): Boolean {
        Log.test(logger,"isConnectionOk")
        return true
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
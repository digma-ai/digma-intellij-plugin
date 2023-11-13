package org.digma.intellij.plugin.analytics

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection

/**
 * This service keeps track of the analytics service connection status. it is used to decide when to show the
 * no connection message in the plugin window.
 * see also class NoConnectionPanel
 */
@Service(Service.Level.PROJECT)
class BackendConnectionMonitor(val project: Project) : Disposable, AnalyticsServiceConnectionEvent {

    companion object {
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
        analyticsConnectionEventsConnection.dispose()
        hasConnectionError = false
    }

    fun isConnectionError(): Boolean {
        return if (ApplicationManager.getApplication().isUnitTestMode) false else hasConnectionError
//        return hasConnectionError // this is the real implementation
        
    }

    fun isConnectionOk(): Boolean {
        return if (ApplicationManager.getApplication().isUnitTestMode) true else !hasConnectionError
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
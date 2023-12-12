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
        // In the unit test mode, there is no real connection to the backend, and we cannot mock it either because it's a light service.
        // So in order for the unit test to successfully run we need to return that there is no connection error.
        return if (ApplicationManager.getApplication().isUnitTestMode) false else hasConnectionError
    }

    fun isConnectionOk(): Boolean {
        // In the unit test mode, there is no real connection to the backend, and we cannot mock it either because it's a light service.
        // So in order for the unit test to successfully run we need to return that the connection is ok.
        return if (ApplicationManager.getApplication().isUnitTestMode) true else !hasConnectionError
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
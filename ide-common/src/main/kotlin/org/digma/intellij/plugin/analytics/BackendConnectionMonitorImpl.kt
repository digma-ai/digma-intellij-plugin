package org.digma.intellij.plugin.analytics

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection

class BackendConnectionMonitorImpl(val project: Project) : Disposable,BackendConnectionMonitor {

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

    override fun isConnectionError(): Boolean {
        return hasConnectionError
    }

    override fun isConnectionOk(): Boolean {
        return !hasConnectionError
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
package org.digma.intellij.plugin.test.system.framework

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor

//System test are running with a mocked backend, so we need to mock the backend connection monitor to simulate a real connection.

class BackendConnectionMonitorTestImpl(project: Project) : Disposable, BackendConnectionMonitor {
    

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

    override fun isConnectionError(): Boolean = false
    

    override fun isConnectionOk(): Boolean = true

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
package org.digma.intellij.plugin.test.system

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.idea.psi.java.JavaCodeLensService
import org.digma.intellij.plugin.ui.service.InsightsViewService

/**
 * This service keeps track of the analytics service connection status. it is used to decide when to show the
 * no connection message in the plugin window.
 * see also class NoConnectionPanel
 */
class BackendConnectionMonitorMock(val project: Project) : Disposable, AnalyticsServiceConnectionEvent {

    companion object {
        private val logger = Logger.getInstance(BackendConnectionMonitorMock::class.java)
        @JvmStatic
        fun getInstance(project: Project): BackendConnectionMonitorMock {
            logger.warn("Getting instance of ${BackendConnectionMonitorMock::class.simpleName}")
            return project.getService(BackendConnectionMonitorMock::class.java)
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
        return hasConnectionError
    }

    fun isConnectionOk(): Boolean {
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
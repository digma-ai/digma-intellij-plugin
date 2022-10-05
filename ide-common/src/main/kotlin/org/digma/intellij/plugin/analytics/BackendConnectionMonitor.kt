package org.digma.intellij.plugin.analytics

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project

/**
 * This service keeps track of the analytics service connection status. it is used to decide when to show the
 * no connection message in the plugin window.
 * see also class NoConnectionWrapper
 */
class BackendConnectionMonitor(val project: Project) : Disposable, AnalyticsServiceConnectionEvent {

    private var hasConnectionError = false


    init {
        project.messageBus.connect(this)
            .subscribe(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC, this)
    }

    override fun dispose() {
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
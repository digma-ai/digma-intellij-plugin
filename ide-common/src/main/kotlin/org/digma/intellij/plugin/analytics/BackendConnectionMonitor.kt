package org.digma.intellij.plugin.analytics

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project

class BackendConnectionMonitor(val project: Project) : Disposable, AnalyticsServiceConnectionEvent {

    private var hasConnectionError = false;


    init {
        project.messageBus.connect(this)
            .subscribe(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC, this)
    }

    override fun dispose() {
        hasConnectionError = false;
    }

    fun isConnectionError(): Boolean {
        return hasConnectionError
    }

    fun isConnectionOk(): Boolean {
        return !hasConnectionError
    }

    fun connectionError() {
        hasConnectionError = true
    }

    fun connectionOk() {
        hasConnectionError = false;
    }

    override fun connectionLost() {
        connectionError()
    }

    override fun connectionGained() {
        connectionOk()
    }


}
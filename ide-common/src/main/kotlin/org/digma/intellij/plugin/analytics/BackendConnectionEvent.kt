package org.digma.intellij.plugin.analytics

import com.intellij.util.messages.Topic


/**
 * this is an application event for connection state.
 * AnalyticsServiceConnectionEvent is a project event, but it actually should be
 * application event because the backend is one for all projects.
 */
//todo: replace AnalyticsServiceConnectionEvent with this event
interface BackendConnectionEvent {
    companion object {
        @JvmStatic
        @Topic.AppLevel
        val BACKEND_CONNECTION_STATE_TOPIC: Topic<BackendConnectionEvent> = Topic.create(
            "BACKEND CONNECTION STATE CHANGED",
            BackendConnectionEvent::class.java
        )
    }

    fun connectionLost()

    fun connectionGained()


}

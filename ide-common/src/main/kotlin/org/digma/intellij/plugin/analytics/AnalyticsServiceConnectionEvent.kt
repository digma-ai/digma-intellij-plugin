package org.digma.intellij.plugin.analytics

import com.intellij.util.messages.Topic

/**
 * An event fired by AnalyticsService when connection is lost or regained.
 * it helps keep track of AnalyticsService availability and present the user with informational messages.
 */
interface AnalyticsServiceConnectionEvent {

    //Note: make sure that only project level services listen to this event and not app level services

    companion object {
        @JvmField
        @Topic.ProjectLevel
        val ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC: Topic<AnalyticsServiceConnectionEvent> =
            Topic.create("ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC", AnalyticsServiceConnectionEvent::class.java)
    }


    fun connectionLost()

    fun connectionGained()

}

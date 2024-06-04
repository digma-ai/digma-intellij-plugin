package org.digma.intellij.plugin.analytics

import com.intellij.util.messages.Topic


interface AnalyticsServiceAppLevelConnectionEvent {

    //Note: make sure that only app level services listen to this event and not project level services

    companion object {
        @JvmStatic
        @Topic.AppLevel
        val ANALYTICS_SERVICE_APP_LEVEL_CONNECTION_EVENT_TOPIC: Topic<AnalyticsServiceAppLevelConnectionEvent> = Topic.create(
            "BACKEND CONNECTION STATE CHANGED",
            AnalyticsServiceAppLevelConnectionEvent::class.java
        )
    }

    fun connectionLost()

    fun connectionGained()


}

package org.digma.intellij.plugin.analytics

import com.intellij.util.messages.Topic


/**
 * this is an application event that should fire when we change the api client,
 * usually when user changes the api url in settings.
 */
fun interface ApiClientChangedAppLevelEvent {

    //Note: make sure that only app level services listen to this event and not project level services

    companion object {
        @JvmStatic
        @Topic.AppLevel
        val API_CLIENT_CHANGED_APP_LEVEL_TOPIC: Topic<ApiClientChangedAppLevelEvent> = Topic.create(
            "API CLIENT CHANGED",
            ApiClientChangedAppLevelEvent::class.java
        )
    }

    fun apiClientChanged(newUrl: String)

}

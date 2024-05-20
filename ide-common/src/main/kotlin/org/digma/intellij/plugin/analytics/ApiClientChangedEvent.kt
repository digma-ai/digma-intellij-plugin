package org.digma.intellij.plugin.analytics

import com.intellij.util.messages.Topic


/**
 * this is an application event that should fire when we change the api client,
 * usually when user changes the api url in settings.
 */
fun interface ApiClientChangedEvent {
    companion object {
        @JvmStatic
        @Topic.AppLevel
        val API_CLIENT_CHANGED_TOPIC: Topic<ApiClientChangedEvent> = Topic.create(
            "API CLIENT CHANGED",
            ApiClientChangedEvent::class.java
        )
    }

    fun apiClientChanged(newUrl: String)

}

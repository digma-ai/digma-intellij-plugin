package org.digma.intellij.plugin.updates.ui

import com.intellij.util.messages.Topic

interface NewUIVersionAvailableEvent {

    companion object {
        @JvmField
        @Topic.AppLevel
        val NEW_UI_VERSION_AVAILABLE_EVENT_TOPIC: Topic<NewUIVersionAvailableEvent> =
            Topic.create("NEW_UI_VERSION_AVAILABLE_EVENT_TOPIC", NewUIVersionAvailableEvent::class.java)
    }


    fun newUIVersionAvailable()

}

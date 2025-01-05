package org.digma.intellij.plugin.updates

import com.intellij.util.messages.Topic


interface UpdateButtonClickedEvent {

    companion object {
        @JvmField
        @Topic.AppLevel
        val UPDATE_BUTTON_CLICKED_EVENT_TOPIC: Topic<UpdateButtonClickedEvent> =
            Topic.create("UPDATE_BUTTON_CLICKED_EVENT_TOPIC", UpdateButtonClickedEvent::class.java)
    }


    fun updateButtonClicked()
}

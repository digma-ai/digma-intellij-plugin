package org.digma.intellij.plugin.ui.jcef.state

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.util.messages.Topic


interface StateChangedEvent {

    companion object {
        @JvmStatic
        @Topic.ProjectLevel
        val JCEF_STATE_CHANGED_TOPIC
                : Topic<StateChangedEvent> = Topic.create(
            "JCEF STATE CHANGED",
            StateChangedEvent::class.java
        )
    }


    fun stateChanged(state: JsonNode)
}
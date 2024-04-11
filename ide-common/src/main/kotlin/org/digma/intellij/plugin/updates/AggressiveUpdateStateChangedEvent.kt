package org.digma.intellij.plugin.updates

import com.intellij.util.messages.Topic

interface AggressiveUpdateStateChangedEvent {

    companion object {
        @JvmStatic
        @Topic.AppLevel
        val UPDATE_STATE_CHANGED_TOPIC: Topic<AggressiveUpdateStateChangedEvent> = Topic.create(
            "UPDATE STATE CHANGED",
            AggressiveUpdateStateChangedEvent::class.java
        )
    }

    fun stateChanged(updateState: PublicUpdateState)

}
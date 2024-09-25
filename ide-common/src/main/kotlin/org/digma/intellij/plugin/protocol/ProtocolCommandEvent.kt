package org.digma.intellij.plugin.protocol

import com.intellij.util.messages.Topic


/**
 * this is an application event that should fire when we change the api client,
 * usually when user changes the api url in settings.
 */
//todo: maybe we don't need it
fun interface ProtocolCommandEvent {


    companion object {
        @JvmStatic
        @Topic.ProjectLevel
        val PROTOCOL_COMMAND_TOPIC: Topic<ProtocolCommandEvent> = Topic.create(
            "PROTOCOL COMMAND",
            ProtocolCommandEvent::class.java
        )
    }

    fun protocolCommand(action: String)

}

package org.digma.intellij.plugin.protocol

import com.intellij.util.messages.Topic



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

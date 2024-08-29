package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.databind.JsonNode
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class GenericPluginEventMessage(val payload: GenericPluginEventPayload) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "GLOBAL/SEND_PLUGIN_EVENT"
}

data class GenericPluginEventPayload(val name: String, val payload: JsonNode?)
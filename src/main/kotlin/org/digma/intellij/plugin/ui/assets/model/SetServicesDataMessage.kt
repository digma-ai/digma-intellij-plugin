package org.digma.intellij.plugin.ui.assets.model

import com.fasterxml.jackson.databind.JsonNode
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class SetServicesDataMessage(val payload: JsonNode) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "ASSETS/SET_SERVICES"
}
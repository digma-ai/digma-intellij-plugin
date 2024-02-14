package org.digma.intellij.plugin.insights.model.outgoing

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.serialization.json.JsonObject
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils

data class SetInsightDataListMessage( @JsonRawValue val payload: String) {
    val type = JCefMessagesUtils.REQUEST_MESSAGE_TYPE
    val action = "INSIGHTS/SET_DATA_LIST"
}
package org.digma.intellij.plugin.insights.model.outgoing

import com.fasterxml.jackson.databind.JsonNode
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils

data class SetInsightDataListMessage(val payload: JsonNode) {
    val type = JCefMessagesUtils.REQUEST_MESSAGE_TYPE
    val action = "INSIGHT/SET_DATA_LIST"
}
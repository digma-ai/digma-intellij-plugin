package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.databind.JsonNode
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils


data class SetStateMessage(val payload: JsonNode?) {
    val type = JCefMessagesUtils.REQUEST_MESSAGE_TYPE
    val action = JCefMessagesUtils.GLOBAL_SET_STATE
}

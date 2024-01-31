package org.digma.intellij.plugin.ui.assets.model

import com.fasterxml.jackson.databind.JsonNode
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils


data class SetCategoriesDataMessage(val payload: JsonNode) {
    val type = JCefMessagesUtils.REQUEST_MESSAGE_TYPE
    val action = "ASSETS/SET_CATEGORIES_DATA"
}
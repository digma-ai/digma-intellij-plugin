package org.digma.intellij.plugin.ui.assets.model

import com.fasterxml.jackson.databind.JsonNode
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants
import org.digma.intellij.plugin.ui.jcef.model.ErrorPayload


data class SetCategoriesDataMessage(val payload: JsonNode,val error: ErrorPayload? = null) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "ASSETS/SET_CATEGORIES_DATA"
}
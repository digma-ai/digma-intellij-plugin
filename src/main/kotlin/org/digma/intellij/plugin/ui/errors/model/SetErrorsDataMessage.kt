package org.digma.intellij.plugin.ui.errors.model

import com.fasterxml.jackson.databind.JsonNode
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

class SetErrorsDataMessage(val payload: JsonNode) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "ERRORS/SET_ERRORS_DATA"
}
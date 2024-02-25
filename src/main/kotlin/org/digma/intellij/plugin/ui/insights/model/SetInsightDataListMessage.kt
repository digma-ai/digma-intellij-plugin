package org.digma.intellij.plugin.ui.insights.model

import com.fasterxml.jackson.annotation.JsonRawValue
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils

data class SetInsightDataListMessage( @JsonRawValue val payload: String) {
    val type = JCefMessagesUtils.REQUEST_MESSAGE_TYPE
    val action = "INSIGHTS/SET_DATA_LIST"
}
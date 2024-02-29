package org.digma.intellij.plugin.ui.insights.model

import com.fasterxml.jackson.annotation.JsonRawValue
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class SetInsightDataListMessage( @JsonRawValue val payload: String) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "INSIGHTS/SET_DATA_LIST"
}
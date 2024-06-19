package org.digma.intellij.plugin.ui.insights.model

import com.fasterxml.jackson.annotation.JsonRawValue
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants
import org.digma.intellij.plugin.ui.jcef.model.ErrorPayload

data class SetInsightDataListMessage( @JsonRawValue val payload: String,val error: ErrorPayload? = null) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "INSIGHTS/SET_DATA_LIST"
}
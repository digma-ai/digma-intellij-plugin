package org.digma.intellij.plugin.ui.insights.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonRawValue
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants
import org.digma.intellij.plugin.ui.jcef.model.ErrorPayload
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SetInsightsData
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("data", "viewMode")
constructor(
    @JsonRawValue val data: String,
    val insightsViewMode: String
)

data class SetInsightDataListMessage(val payload: SetInsightsData, val error: ErrorPayload? = null) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "INSIGHTS/SET_DATA_LIST"
}
package org.digma.intellij.plugin.ui.insights.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SetInsightsAsReadData @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("insightIds", "status", "error")
constructor(
    val insightIds: List<String>,
    val status: String,
    val error: String?,
)

data class SetInsightsMarkAsReadMessage(val payload: SetInsightsAsReadData) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "INSIGHTS/SET_MARK_AS_READ_RESPONSE"
}
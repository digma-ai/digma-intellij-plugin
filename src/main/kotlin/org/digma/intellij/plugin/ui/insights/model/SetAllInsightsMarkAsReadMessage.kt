package org.digma.intellij.plugin.ui.insights.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SetAllInsightsAsReadData @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("scope", "status", "error")
constructor(
    val scope: JsonNode?,
    val status: String,
    val error: String?,
)

data class SetAllInsightsMarkAsReadMessage(val payload: SetAllInsightsAsReadData) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "INSIGHTS/SET_MARK_ALL_AS_READ_RESPONSE"
}


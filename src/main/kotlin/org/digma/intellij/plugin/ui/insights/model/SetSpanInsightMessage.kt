package org.digma.intellij.plugin.ui.insights.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SetSpanInsightData @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("insight")
constructor(
    val insight: JsonNode?,
)

data class SetSpanInsightMessage(val type: String = "digma", val action: String = "INSIGHTS/SET_SPAN_INSIGHT", val payload: SetSpanInsightData)

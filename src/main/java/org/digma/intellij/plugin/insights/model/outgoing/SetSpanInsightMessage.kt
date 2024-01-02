package org.digma.intellij.plugin.insights.model.outgoing

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SetSpanInsightData @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("insight")
constructor(
    val insight: CodeObjectInsight ?,
)

data class SetSpanInsightMessage(val type: String = "digma", val action: String = "INSIGHTS/SET_SPAN_INSIGHT", val payload: SetSpanInsightData)

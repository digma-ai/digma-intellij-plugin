package org.digma.intellij.plugin.ui.insights.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants
import java.beans.ConstructorProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class SetInsightRecalculated @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("insightId")
constructor(
    val insightId: String,
)

data class SetInsightRecalculatedMessage(val payload: SetInsightRecalculated) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "INSIGHTS/SET_RECALCULATED"
}


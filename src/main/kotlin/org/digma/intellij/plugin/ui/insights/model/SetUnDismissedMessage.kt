package org.digma.intellij.plugin.ui.insights.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SetUnDismissedData @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("insightId", "status", "error")
constructor(
    val insightId: String,
    val status: String,
    val error: String?
)

data class SetUnDismissedMessage(val payload: SetUnDismissedData) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "INSIGHTS/SET_UNDISMISS_RESPONSE"
}
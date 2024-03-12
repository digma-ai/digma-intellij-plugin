package org.digma.intellij.plugin.ui.insights.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SetDismissedData @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("insightId", "status","error")
constructor(
    val insightId: String,
    val status: String,
    var error: String?
)

data class SetDismissedMessage(val payload: SetDismissedData) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "INSIGHTS/SET_DISMISS_RESPONSE"
}
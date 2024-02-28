package org.digma.intellij.plugin.ui.wizard.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants
import java.beans.ConstructorProperties


data class ConnectionCheckMessageRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("payload")
constructor(
    val payload: ConnectionCheckMessagePayload?,
) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = JCEFGlobalConstants.INSTALLATION_WIZARD_SET_CHECK_CONNECTION
}


data class ConnectionCheckMessagePayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val result: String)


enum class ConnectionCheckResult(val value: String) {
    SUCCESS("success"),
    FAILURE("failure")
}
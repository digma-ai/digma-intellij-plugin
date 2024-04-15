package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.annotation.JsonRawValue
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class SetRegistrationMessage(
    @JsonRawValue val payload: String,
) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "GLOBAL/SET_REGISTRATION_RESULT"
}

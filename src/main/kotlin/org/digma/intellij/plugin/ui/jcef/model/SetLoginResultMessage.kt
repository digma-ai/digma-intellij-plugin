package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.annotation.JsonRawValue
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class SetLoginResultMessage(
    val payload: LoginResultPayload,
) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "GLOBAL/LOGIN_RESULT"
}

data class LoginResultPayload(val isSuccess: Boolean, @JsonRawValue val errors:String? )
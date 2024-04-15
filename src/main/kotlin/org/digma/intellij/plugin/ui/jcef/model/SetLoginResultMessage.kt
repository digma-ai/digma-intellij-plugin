package org.digma.intellij.plugin.ui.jcef.model

import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class SetLoginResultMessage(
    val payload: LoginResultPayload,
) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "GLOBAL/SET_LOGIN_RESULT"
}

data class LoginResultPayload(val isSuccess: Boolean, val error: String?)
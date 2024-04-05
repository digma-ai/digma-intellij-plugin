package org.digma.intellij.plugin.ui.jcef.model

import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class SetUserInfoMessage(
    val payload: UserInfoPayload,
) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "GLOBAL/SET_USER_INFO"
}

data class UserInfoPayload(val id: String?)
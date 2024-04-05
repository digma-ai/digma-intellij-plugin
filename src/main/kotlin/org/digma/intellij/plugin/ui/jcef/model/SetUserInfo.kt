package org.digma.intellij.plugin.ui.jcef.model

data class SetUserInfoMessage(
    val type: String,
    val action: String,
    val payload: UserInfoPayload,
)

data class UserInfoPayload(val id: String?)
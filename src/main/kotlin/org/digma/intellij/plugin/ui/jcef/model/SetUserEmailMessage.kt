package org.digma.intellij.plugin.ui.jcef.model

data class SetUserEmailMessage(
    val type: String,
    val action: String,
    val payload: UserEmailPayload,
)

data class UserEmailPayload(val email: String)
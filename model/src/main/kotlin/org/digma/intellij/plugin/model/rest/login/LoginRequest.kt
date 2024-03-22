package org.digma.intellij.plugin.model.rest.login

data class LoginRequest(
    val username: String,
    val email: String?,
    val password: String,
)

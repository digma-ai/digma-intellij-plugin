package org.digma.intellij.plugin.auth

data class LoginResult(val isSuccess: Boolean, val userId: String?, val error: String?)
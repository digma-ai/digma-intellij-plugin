package org.digma.intellij.plugin.auth.account

data class LoginResult(val isSuccess: Boolean, val userId: String?, val error: String?)
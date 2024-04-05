package org.digma.intellij.plugin.auth

data class AuthInfo(val userId: String?);

fun interface AuthInfoChangeListener {
    fun authInfoChanged(authInfo: AuthInfo)
}
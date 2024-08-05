package org.digma.intellij.plugin.auth

//AuthManager relies on equality of AuthInfo to decide if to fire authInfoChanged event.
// remember that when changing this class
data class AuthInfo(val userId: String?, val accountId: String?)

fun interface AuthInfoChangeListener {
    fun authInfoChanged(authInfo: AuthInfo)
}
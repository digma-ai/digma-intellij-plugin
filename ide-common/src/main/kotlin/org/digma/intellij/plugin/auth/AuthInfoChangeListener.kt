package org.digma.intellij.plugin.auth

import org.digma.intellij.plugin.auth.credentials.AuthInfo

interface AuthInfoChangeListener {
    fun authInfoChanged(authInfo: AuthInfo)
}
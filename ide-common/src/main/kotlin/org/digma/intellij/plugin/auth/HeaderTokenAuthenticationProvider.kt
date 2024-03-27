package org.digma.intellij.plugin.auth

import org.digma.intellij.plugin.analytics.AuthenticationProvider

class HeaderTokenAuthenticationProvider(private val tokenProvider: TokenProvider) : AuthenticationProvider {

    override fun getHeaderName(): String {
        return "Authorization"
    }

    override fun getHeaderValue(): String? {
        return tokenProvider.provideToken()
    }

}
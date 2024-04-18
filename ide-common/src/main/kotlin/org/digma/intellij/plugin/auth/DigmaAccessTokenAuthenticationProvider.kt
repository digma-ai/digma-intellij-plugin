package org.digma.intellij.plugin.auth

import org.digma.intellij.plugin.analytics.AuthenticationProvider

class DigmaAccessTokenAuthenticationProvider(private val tokenProvider: TokenProvider) : AuthenticationProvider {

    override fun getHeaderName(): String {
        return "Digma-Access-Token"
    }

    override fun getHeaderValue(): String? {
        return tokenProvider.provideToken()
    }

}
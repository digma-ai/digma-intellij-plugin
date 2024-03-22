package org.digma.intellij.plugin.auth

import kotlinx.coroutines.runBlocking
import org.digma.intellij.plugin.auth.account.DigmaAccountManager
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
import org.digma.intellij.plugin.settings.SettingsState


enum class TokenType { Bearer, Token }

interface TokenProvider {
    fun provideToken(): String?
}


class SettingsTokenProvider : TokenProvider {
    override fun provideToken(): String? {
        return SettingsState.getInstance().apiToken?.let {
            "${TokenType.Token.name} $it"
        }
    }
}


class DefaultAccountTokenProvider : TokenProvider {

    //todo: consider caching the token. on refresh or when the api client is replaced this object will be replaced too
    override fun provideToken(): String? {
        return runBlocking {
            val account = DigmaDefaultAccountHolder.getInstance().account
            account?.let {
                val credentials = DigmaAccountManager.getInstance().findCredentials(account)
                credentials?.let {
                    "${TokenType.Bearer.name} ${it.accessToken}"
                }
            }
        }
    }
}
package org.digma.intellij.plugin.auth

import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.runBlocking
import org.digma.intellij.plugin.auth.account.DigmaAccountManager
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.settings.SettingsState


enum class TokenType { Bearer, Token }

interface TokenProvider {
    fun provideToken(): String?
}

//always takes the token from settings
class SettingsTokenProvider : TokenProvider {
    override fun provideToken(): String? {
        return SettingsState.getInstance().apiToken?.let {
            "${TokenType.Token.name} $it"
        }
    }
}


//takes the token from default account if exists
class DefaultAccountTokenProvider : TokenProvider {

    private val logger = Logger.getInstance(this::class.java)

    override fun provideToken(): String? {
        return try {
            runBlocking {
                val account = DigmaDefaultAccountHolder.getInstance().account
                account?.let {
                    val credentials = DigmaAccountManager.getInstance().findCredentials(account)
                    credentials?.let { creds ->
                        "${creds.tokenType} ${creds.accessToken}"
                    }
                }
            }
        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "Unable to provide token")
            ErrorReporter.getInstance().reportInternalFatalError("DefaultAccountTokenProvider.provideToken", e)
            null
        }
    }
}
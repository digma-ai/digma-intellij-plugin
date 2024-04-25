package org.digma.intellij.plugin.auth

import kotlinx.datetime.Clock
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.auth.account.DigmaAccount
import org.digma.intellij.plugin.auth.account.DigmaAccountManager
import org.digma.intellij.plugin.auth.credentials.DigmaCredentials
import org.digma.intellij.plugin.model.rest.login.LoginRequest
import org.digma.intellij.plugin.model.rest.login.RefreshRequest


class AuthApiClient(private val analyticsProvider: RestAnalyticsProvider) {

    @Synchronized
    fun login(user: String, password: String): LoginResult {

        val loginResponse = analyticsProvider.login(LoginRequest(user, password))

        val digmaAccount = DigmaAccountManager.createAccount(analyticsProvider.apiUrl, loginResponse.userId)

        val digmaCredentials = DigmaCredentials(
            loginResponse.accessToken,
            loginResponse.refreshToken,
            analyticsProvider.apiUrl,
            TokenType.Bearer.name,
            loginResponse.expiration.time,
            Clock.System.now().toEpochMilliseconds()
        )

        updateAccount(digmaAccount, digmaCredentials)

        return LoginResult(true, loginResponse.userId, null)
    }


    @Synchronized
    fun refreshToken(
        digmaAccount: DigmaAccount,
        credentials: DigmaCredentials
    ): Boolean {

        val loginResponse = analyticsProvider.refreshToken(RefreshRequest(credentials.accessToken, credentials.refreshToken))

        val digmaCredentials = DigmaCredentials(
            loginResponse.accessToken,
            loginResponse.refreshToken,
            digmaAccount.server.url,
            TokenType.Bearer.name,
            loginResponse.expiration.time,
            Clock.System.now().toEpochMilliseconds()
        )

        updateAccount(digmaAccount, digmaCredentials)

        return true

    }

}
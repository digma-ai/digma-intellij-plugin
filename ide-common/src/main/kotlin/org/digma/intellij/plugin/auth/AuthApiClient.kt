package org.digma.intellij.plugin.auth

import kotlinx.datetime.Clock
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.auth.account.DigmaAccount
import org.digma.intellij.plugin.auth.credentials.DigmaCredentials
import org.digma.intellij.plugin.model.rest.login.LoginRequest
import org.digma.intellij.plugin.model.rest.login.RefreshRequest

//these are the two API for login and refresh
class AuthApiClient(private val analyticsProvider: RestAnalyticsProvider) {

    fun login(user: String, password: String): DigmaCredentials {

        val loginResponse = analyticsProvider.login(LoginRequest(user, password))

        return DigmaCredentials(
            loginResponse.userId,
            loginResponse.accessToken,
            loginResponse.refreshToken,
            analyticsProvider.apiUrl,
            TokenType.Bearer.name,
            loginResponse.expiration.time,
            Clock.System.now().toEpochMilliseconds()
        )
    }


    fun refreshToken(
        digmaAccount: DigmaAccount,
        credentials: DigmaCredentials
    ): DigmaCredentials {

        val loginResponse = analyticsProvider.refreshToken(RefreshRequest(credentials.accessToken, credentials.refreshToken))

        return DigmaCredentials(
            loginResponse.userId,
            loginResponse.accessToken,
            loginResponse.refreshToken,
            digmaAccount.server.url,
            TokenType.Bearer.name,
            loginResponse.expiration.time,
            Clock.System.now().toEpochMilliseconds()
        )
    }

}
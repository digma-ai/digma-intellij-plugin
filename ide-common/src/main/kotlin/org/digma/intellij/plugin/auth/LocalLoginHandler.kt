package org.digma.intellij.plugin.auth

import kotlinx.coroutines.runBlocking
import org.digma.intellij.plugin.analytics.AuthenticationException
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.auth.account.DigmaAccountManager
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.scheduling.blockingOneShotTaskWithResult
import kotlin.time.Duration.Companion.seconds


private const val SILENT_LOGIN_USER = "admin@digma.ai"
private const val SILENT_LOGIN_PASSWORD = "admin!"

class LocalLoginHandler(analyticsProvider: RestAnalyticsProvider) : AbstractLoginHandler(analyticsProvider) {


    override fun loginOrRefresh(onAuthenticationError: Boolean): Boolean {

        return try {

            Log.log(logger::trace, "loginOrRefresh called, url: {}", analyticsProvider.apiUrl)

            val digmaAccount = getDefaultAccount()

            if (digmaAccount == null) {
                Log.log(logger::trace, "no account found in loginOrRefresh, doing silent login for url: {}", analyticsProvider.apiUrl)
                val loginResult = login(SILENT_LOGIN_USER, SILENT_LOGIN_PASSWORD)
                if (loginResult.isSuccess) {
                    Log.log(logger::trace, "silent login success for url: {}, account {}", analyticsProvider.apiUrl, getDefaultAccount())
                } else {
                    Log.log(logger::trace, "silent login failed for url: {}, error {}", analyticsProvider.apiUrl, loginResult.error)
                }
                loginResult.isSuccess
            } else if (digmaAccount.server.url != analyticsProvider.apiUrl) {
                Log.log(
                    logger::warn, "digma account exists,but its url is different from analytics url, deleting account {}, url {}",
                    digmaAccount,
                    analyticsProvider.apiUrl
                )
                logout()
                Log.log(logger::trace, "doing silent login for url: {}", analyticsProvider.apiUrl)
                val loginResult = login(SILENT_LOGIN_USER, SILENT_LOGIN_PASSWORD)
                if (loginResult.isSuccess) {
                    Log.log(logger::trace, "silent login success for url: {}, account {}", analyticsProvider.apiUrl, getDefaultAccount())
                } else {
                    Log.log(logger::trace, "silent login failed for url: {}, error {}", analyticsProvider.apiUrl, loginResult.error)
                }
                loginResult.isSuccess
            } else {

                Log.log(logger::trace, "found account in loginOrRefresh, account: {}", digmaAccount)

                val credentials = blockingOneShotTaskWithResult("AuthManager.LocalLoginHandler.findCredentials", 2.seconds.inWholeMilliseconds) {
                    runBlocking {
                        val creds = DigmaAccountManager.getInstance().findCredentials(digmaAccount)
                        CredentialsHolder.digmaCredentials = creds
                        creds
                    }
                }


                //if digma account is not null and credentials is null then probably something corrupted,
                // it may be that the credentials deleted from the password safe
                if (credentials == null) {
                    Log.log(
                        logger::warn,
                        "no credentials found for account {}, maybe credentials deleted from password safe? deleting account, analytics url {}",
                        digmaAccount,
                        analyticsProvider.apiUrl
                    )

                    logout()
                    Log.log(logger::trace, "doing silent login for url: {}", analyticsProvider.apiUrl)
                    val loginResult = login(SILENT_LOGIN_USER, SILENT_LOGIN_PASSWORD)
                    if (loginResult.isSuccess) {
                        Log.log(logger::trace, "silent login success for url: {}, account {}", analyticsProvider.apiUrl, getDefaultAccount())
                    } else {
                        Log.log(logger::trace, "silent login failed for url: {}, error {}", analyticsProvider.apiUrl, loginResult.error)
                    }
                    loginResult.isSuccess
                } else {

                    Log.log(logger::trace, "found credentials for account {}", digmaAccount)

                    if (!credentials.isAccessTokenValid()) {
                        Log.log(logger::trace, "access token for account expired, refreshing token. account {}", digmaAccount)
                        val result = authApiClient.refreshToken(digmaAccount, credentials)
                        Log.log(logger::trace, "refresh token success for account {}", digmaAccount)
                        result
                    } else if (onAuthenticationError && credentials.isOlderThen(30.seconds)) {

                        //why check isOlderThen(30.seconds)?
                        //to prevent multiple unnecessary refresh token.
                        //when credentials expired multiple threads will fail on authentication error.
                        //all these threads will try to refresh the token.
                        //see: AuthManager.onAuthenticationException
                        //but of course only one refresh is necessary.
                        //if these threads try to refresh on the same time, they will wait in turn because
                        //AuthManager.onAuthenticationException is synchronized.
                        //and then when this code is executed it will not refresh again if the credentials are valid
                        //and were refresh in the past 30 seconds

                        Log.log(
                            logger::trace,
                            "onAuthenticationError is true and credentials older then 30 seconds, refreshing token for account {}",
                            digmaAccount
                        )
                        val result = authApiClient.refreshToken(digmaAccount, credentials)
                        Log.log(logger::trace, "refresh token success for account {}", digmaAccount)
                        result
                    } else {
                        Log.log(logger::trace, "no need to refresh token for account {}", digmaAccount)
                        true
                    }
                }
            }
        } catch (e: Throwable) {

            Log.warnWithException(logger, e, "Exception in loginOrRefresh {}, url {}", e, analyticsProvider.apiUrl)
            ErrorReporter.getInstance().reportError("LocalLoginHandler.loginOrRefresh", e)

            //if got AuthenticationException here is may be from refresh or login, in both cases delete the current account,
            //and we'll do silent login on the next loginOrRefresh
            if (e is AuthenticationException) {
                val errorMessage = ExceptionUtils.getNonEmptyMessage(e)
                reportPosthogEvent("loginOrRefresh failed", mapOf("error" to errorMessage))
                logout()
            }

            false
        }

    }

}
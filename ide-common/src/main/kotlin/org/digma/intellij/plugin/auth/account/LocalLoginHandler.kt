package org.digma.intellij.plugin.auth.account

import kotlinx.coroutines.CoroutineName
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.auth.reportAuthPosthogEvent
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

private const val SILENT_LOGIN_USER = "admin@digma.ai"
private const val SILENT_LOGIN_PASSWORD = "admin!"


class LocalLoginHandler(analyticsProvider: RestAnalyticsProvider) : AbstractLoginHandler(analyticsProvider) {

    //make sure CredentialsHolder.digmaCredentials is always updated correctly
    override suspend fun loginOrRefresh(onAuthenticationError: Boolean, trigger: String): Boolean {

        return try {

            trace("loginOrRefresh called, url: {},trigger {}", analyticsProvider.apiUrl, trigger)

            reportAuthPosthogEvent("loginOrRefresh", this.javaClass.simpleName, mapOf("loginOrRefresh trigger" to trigger))

            val digmaAccount = getDefaultAccount()

            if (digmaAccount == null) {
                trace("no account found in loginOrRefresh, doing silent login for url: {}", analyticsProvider.apiUrl)
                val loginResult = login(SILENT_LOGIN_USER, SILENT_LOGIN_PASSWORD, "${this.javaClass.simpleName} silent login because no account")
                if (loginResult.isSuccess) {
                    trace("silent login success for url: {}, account {}", analyticsProvider.apiUrl, getDefaultAccount())
                } else {
                    trace("silent login failed for url: {}, error {}", analyticsProvider.apiUrl, loginResult.error)
                }
                loginResult.isSuccess
            } else if (digmaAccount.server.url != analyticsProvider.apiUrl) {
                trace(
                    "${coroutineContext[CoroutineName]}: digma account exists,but its url is different from analytics url, deleting account {}, url {}",
                    digmaAccount,
                    analyticsProvider.apiUrl
                )
                logout("${this.javaClass.simpleName}: account url different from analytics url")
                trace("doing silent login for url: {}", analyticsProvider.apiUrl)
                val loginResult = login(
                    SILENT_LOGIN_USER,
                    SILENT_LOGIN_PASSWORD,
                    "${this.javaClass.simpleName} silent login because account url different from analytics url"
                )
                if (loginResult.isSuccess) {
                    trace("silent login success for url: {}, account {}", analyticsProvider.apiUrl, getDefaultAccount())
                } else {
                    trace("silent login failed for url: {}, error {}", analyticsProvider.apiUrl, loginResult.error)
                }
                loginResult.isSuccess
            } else {

                trace("found account in loginOrRefresh, account: {}", digmaAccount)

                val credentials = try {
                    //exception here may happen if we change the credentials structure,which doesn't happen too much,
                    // we'll do a silent login again
                    DigmaAccountManager.getInstance().findCredentials(digmaAccount)
                } catch (_: Throwable) {
                    null
                }

                //if digma account is not null and credentials is null then probably something corrupted,
                // it may be that the credentials deleted from the password safe
                if (credentials == null) {
                    trace(
                        "no credentials found for account {}, maybe credentials deleted from password safe? deleting account, analytics url {}",
                        digmaAccount,
                        analyticsProvider.apiUrl
                    )
                    logout("${this.javaClass.simpleName}: no credentials for account")
                    trace("doing silent login for url: {}", analyticsProvider.apiUrl)
                    val loginResult = login(
                        SILENT_LOGIN_USER,
                        SILENT_LOGIN_PASSWORD,
                        "${this.javaClass.simpleName} silent login because account exists but no credentials"
                    )
                    if (loginResult.isSuccess) {
                        trace("silent login success for url: {}, account {}", analyticsProvider.apiUrl, getDefaultAccount())
                    } else {
                        trace("silent login failed for url: {}, error {}", analyticsProvider.apiUrl, loginResult.error)
                    }
                    loginResult.isSuccess
                } else {

                    trace("found credentials for account {}", digmaAccount)

                    if (!credentials.isAccessTokenValid()) {
                        trace("access token for account expired, refreshing token. account {}", digmaAccount)
                        val refreshResult = refresh(digmaAccount, credentials, "${this.javaClass.simpleName} token expired")
                        trace("refresh token completed for account {}, result {}", digmaAccount, refreshResult)
                        refreshResult
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

                        trace("onAuthenticationError is true and credentials older then 30 seconds, refreshing token for account {}", digmaAccount)
                        val refreshResult =
                            refresh(digmaAccount, credentials, "${this.javaClass.simpleName} on onAuthenticationError and token is old")
                        trace("refresh token completed for account {},result {}", digmaAccount, refreshResult)
                        refreshResult
                    } else {
                        trace("no need to refresh token for account {}", digmaAccount)
                        //found credentials and its valid, probably on startup, update CredentialsHolder
                        CredentialsHolder.digmaCredentials = credentials
                        true
                    }
                }
            }
        } catch (e: Throwable) {

            warnWithException(e, "Exception in loginOrRefresh {}, url {}", e, analyticsProvider.apiUrl)
            ErrorReporter.getInstance().reportError("${javaClass.simpleName}.loginOrRefresh", e, mapOf("loginOrRefresh trigger" to trigger))
            val errorMessage = ExceptionUtils.getNonEmptyMessage(e)
            reportAuthPosthogEvent(
                "loginOrRefresh failed",
                this.javaClass.simpleName,
                mapOf("error" to errorMessage, "loginOrRefresh trigger" to trigger)
            )

            //if got exception here it may be from refresh or login, in both cases delete the current account
            //and login again
            logout("${this.javaClass.simpleName}: error in loginOrRefresh $e")
            login(SILENT_LOGIN_USER, SILENT_LOGIN_PASSWORD, "${this.javaClass.simpleName} silent login because of error $e")

            false
        } finally {
            reportAuthPosthogEvent("loginOrRefresh completed", this.javaClass.simpleName, mapOf("loginOrRefresh trigger" to trigger))
        }

    }

}
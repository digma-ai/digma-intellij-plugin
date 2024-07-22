package org.digma.intellij.plugin.auth.account

import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import kotlin.time.Duration.Companion.seconds

class CentralizedLoginHandler(analyticsProvider: RestAnalyticsProvider) : AbstractLoginHandler(analyticsProvider) {


    //make sure CredentialsHolder.digmaCredentials is always updated correctly
    override suspend fun loginOrRefresh(onAuthenticationError: Boolean): Boolean {

        return try {

            Log.log(logger::trace, "loginOrRefresh called, url: {}", analyticsProvider.apiUrl)

            val digmaAccount = getDefaultAccount()

            if (digmaAccount == null) {
                CredentialsHolder.digmaCredentials = null
                Log.log(
                    logger::trace,
                    "no account found in loginOrRefresh, account is not logged in for centralized env url: {}",
                    analyticsProvider.apiUrl
                )
                false
            } else if (digmaAccount.server.url != analyticsProvider.apiUrl) {
                Log.log(
                    logger::warn, "digma account exists,but its url is different from analytics url, deleting account {}, url {}",
                    digmaAccount,
                    analyticsProvider.apiUrl
                )
                logout()
                false
            } else {

                Log.log(logger::trace, "found account in loginOrRefresh, account: {}", digmaAccount)

                val credentials = try {
                    //exception here may happen if we change the credentials structure,which doesn't happen too much,
                    // user will be redirected to log in again
                    DigmaAccountManager.getInstance().findCredentials(digmaAccount)
                } catch (_: Throwable) {
                    null
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
                    false
                } else {

                    Log.log(logger::trace, "found credentials for account {}", digmaAccount)

                    if (!credentials.isAccessTokenValid()) {
                        Log.log(logger::trace, "access token for account expired, refreshing token. account {}", digmaAccount)
                        val refreshResult = refresh(digmaAccount, credentials)
                        Log.log(logger::trace, "refresh token success for account {}", digmaAccount)
                        refreshResult
                    } else if (onAuthenticationError && credentials.isOlderThen(30.seconds)) {

                        //why check isOlderThen(30.seconds)?
                        //to prevent multiple unnecessary refresh token.
                        //when credentials expired multiple threads will fail on authentication error.
                        //maybe few threads will try to refresh the token.
                        //but of course only one refresh is necessary.
                        //if these threads try to refresh one after the other than when this code is
                        // executed it will not refresh again if the credentials are valid and were refreshed
                        // in the past 30 seconds

                        Log.log(
                            logger::trace,
                            "onAuthenticationError is true and credentials older then 30 seconds, refreshing token. account {}",
                            digmaAccount
                        )
                        val refreshResult = refresh(digmaAccount, credentials)
                        Log.log(logger::trace, "refresh token success for account {}", digmaAccount)
                        refreshResult
                    } else {
                        Log.log(logger::trace, "no need to refresh token for account {}", digmaAccount)
                        //found credentials and its valid, probably on startup, update CredentialsHolder
                        CredentialsHolder.digmaCredentials = credentials
                        true
                    }
                }
            }
        } catch (e: Throwable) {

            Log.warnWithException(logger, e, "Exception in loginOrRefresh {}, url {}", e, analyticsProvider.apiUrl)
            ErrorReporter.getInstance().reportError("CentralizedLoginHandler.loginOrRefresh", e)
            val errorMessage = ExceptionUtils.getNonEmptyMessage(e)
            reportPosthogEvent("loginOrRefresh failed", mapOf("error" to errorMessage))

            //if got exception here it may be from refresh or login, in both cases delete the current account,
            //and user will be redirected to log in again
            logout()

            false
        }

    }


}
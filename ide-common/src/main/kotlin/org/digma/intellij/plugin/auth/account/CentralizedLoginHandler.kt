package org.digma.intellij.plugin.auth.account

import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.auth.reportAuthPosthogEvent
import org.digma.intellij.plugin.auth.withAuthManagerDebug
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import kotlin.time.Duration.Companion.seconds

class CentralizedLoginHandler(analyticsProvider: RestAnalyticsProvider) : AbstractLoginHandler(analyticsProvider) {


    //make sure CredentialsHolder.digmaCredentials is always updated correctly
    override suspend fun loginOrRefresh(onAuthenticationError: Boolean, trigger: String): Boolean {

        return try {

            trace("loginOrRefresh called, url: {},trigger {}", analyticsProvider.apiUrl, trigger)

            withAuthManagerDebug {
                reportAuthPosthogEvent("loginOrRefresh", this.javaClass.simpleName, trigger)
            }

            val digmaAccount = getDefaultAccount()

            if (digmaAccount == null) {
                CredentialsHolder.digmaCredentials = null
                trace("no account found in loginOrRefresh, account is not logged in for centralized env url: {}", analyticsProvider.apiUrl)
                false
            } else if (digmaAccount.server.url != analyticsProvider.apiUrl) {
                trace(
                    "digma account exists,but its url is different from analytics url, deleting account {}, url {}",
                    digmaAccount,
                    analyticsProvider.apiUrl
                )
                logout("${this.javaClass.simpleName}: account url different from analytics url,$trigger")
                false
            } else {

                trace("found account in loginOrRefresh, account: {}", digmaAccount)

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
                    trace(
                        "no credentials found for account {}, maybe credentials deleted from password safe? deleting account, analytics url {}",
                        digmaAccount,
                        analyticsProvider.apiUrl
                    )
                    logout("${this.javaClass.simpleName}: no credentials for account,$trigger")
                    false
                } else {

                    trace("found credentials for account {}", digmaAccount)

                    if (!credentials.isAccessTokenValid()) {
                        trace("access token for account expired, refreshing token. account {}", digmaAccount)
                        val refreshResult = refresh(digmaAccount, credentials, "${this.javaClass.simpleName} token expired,$trigger")
                        trace("refresh token completed for account {}, result {}", digmaAccount, refreshResult)
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

                        trace("onAuthenticationError is true and credentials older then 30 seconds, refreshing token. account {}", digmaAccount)
                        val refreshResult =
                            refresh(digmaAccount, credentials, "${this.javaClass.simpleName} on onAuthenticationError and token is old,$trigger")
                        trace("refresh token completed for account {}, result {}", digmaAccount, refreshResult)
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

            withAuthManagerDebug {
                val errorMessage = ExceptionUtils.getNonEmptyMessage(e)
                reportAuthPosthogEvent(
                    "loginOrRefresh failed",
                    this.javaClass.simpleName,
                    trigger,
                    mapOf("error" to errorMessage)
                )
            }

            //if got exception here then we probably can't refresh,logout, user will be redirected to login,
            // throw the exception to report it
            logout("${this.javaClass.simpleName}: error in loginOrRefresh $e,$trigger")
            throw e
        } finally {
            withAuthManagerDebug {
                reportAuthPosthogEvent("loginOrRefresh completed", this.javaClass.simpleName, trigger)
            }
        }

    }


}
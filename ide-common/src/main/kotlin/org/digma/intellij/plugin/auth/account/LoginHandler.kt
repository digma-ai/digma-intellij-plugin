package org.digma.intellij.plugin.auth.account

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.ApiErrorHandler
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.auth.NoOpLoginHandler
import org.digma.intellij.plugin.auth.credentials.DigmaCredentials
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor

interface LoginHandler {

    val logger: Logger

    companion object {

        private val logger: Logger = Logger.getInstance(this::class.java)

        fun createLoginHandler(analyticsProvider: RestAnalyticsProvider): LoginHandler {
            return createLoginHandler(null, analyticsProvider)
        }

        fun createLoginHandler(project: Project?, analyticsProvider: RestAnalyticsProvider): LoginHandler {

            try {

                Log.log(logger::trace, "createLoginHandler called for url {}", analyticsProvider.apiUrl)

                val loginHandler = when (analyticsProvider.about.isCentralize) {
                    true -> CentralizedLoginHandler(analyticsProvider)
                    false, null -> LocalLoginHandler(analyticsProvider)
                }

                //reset connection status in case it's in no connection mode,
                //resetConnectionLostAndNotifyIfNecessary is a fast method when the connection is ok
                ApiErrorHandler.getInstance().resetConnectionLostAndNotifyIfNecessary(project)

                Log.log(logger::trace, "created {} for url {}", loginHandler, analyticsProvider.apiUrl)
                return loginHandler

            } catch (e: Throwable) {

                //if we can't create a login handler we assume there is a connection issue, it may be a real connect issue,
                // or any other issue were we can't get analyticsProvider.about
                ApiErrorHandler.getInstance().handleAuthManagerError(e, project)

                Log.warnWithException(logger, e, "Exception in createLoginHandler {}, url {}", e, analyticsProvider.apiUrl)
                ErrorReporter.getInstance().reportError("AuthManager.createLoginHandler", e)

                Log.log(logger::trace, "Got exception in createLoginHandler , returning NoOpLoginHandler", analyticsProvider.apiUrl)
                return NoOpLoginHandler("error in createLoginHandler $e")
            }
        }
    }


    suspend fun login(user: String, password: String): LoginResult

    //does login or refresh if necessary, return true if did successful login or successful refresh,
    // or when no need to do anything. return false if failed.
    //we don't really rely on the returned value, mostly for logging
    suspend fun loginOrRefresh(onAuthenticationError: Boolean = false): Boolean


    suspend fun logout(): Boolean {
        return try {
            Log.log(logger::trace, "logout called")

            val digmaAccount = DigmaDefaultAccountHolder.getInstance().account

            Log.log(logger::trace, "logout: found account {}", digmaAccount)

            digmaAccount?.let { account ->
                deleteAccount(account)
            }

            digmaAccount?.let {
                Log.log(logger::trace, "logout: account deleted {} ", digmaAccount)
            }

            //return true only if an account was really deleted
            digmaAccount != null

        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "Exception in logout {}", e)
            ErrorReporter.getInstance().reportError("AuthManager.logout", e)
            false
        }
    }


    suspend fun updateAccount(digmaAccount: DigmaAccount, digmaCredentials: DigmaCredentials) {
        //this is the only place we update the account and credentials.
        DigmaAccountManager.getInstance().updateAccount(digmaAccount, digmaCredentials)
        DigmaDefaultAccountHolder.getInstance().account = digmaAccount
        CredentialsHolder.digmaCredentials = digmaCredentials
    }


    suspend fun deleteAccount(account: DigmaAccount) {
        //this is the only place we delete the account.
        try {
            DigmaAccountManager.getInstance().removeAccount(account)
        } catch (e: Throwable) {
            throw e
        } finally {
            DigmaDefaultAccountHolder.getInstance().account = null
            CredentialsHolder.digmaCredentials = null
        }
    }


    fun reportPosthogEvent(evenName: String, details: Map<String, String> = mapOf()) {
        findActiveProject()?.let { project ->
            ActivityMonitor.getInstance(project).registerCustomEvent(evenName, details)
        }
    }

}
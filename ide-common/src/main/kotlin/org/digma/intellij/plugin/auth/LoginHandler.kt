package org.digma.intellij.plugin.auth

import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.runBlocking
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.auth.account.DigmaAccountManager
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log

interface LoginHandler {

    val logger: Logger

    companion object {

        private val logger: Logger = Logger.getInstance(LoginHandler::class.java)

        fun createLoginHandler(analyticsProvider: RestAnalyticsProvider): LoginHandler {

            return try {

                when (analyticsProvider.about.isCentralize) {
                    true -> CentralizedLoginHandler(analyticsProvider)
                    false, null -> LocalLoginHandler(analyticsProvider)
                }

            } catch (e: Throwable) {

                Log.warnWithException(logger, e, "Exception in createLoginHandler {}, url {}", e, analyticsProvider.apiUrl)
                ErrorReporter.getInstance().reportError("AuthManager.createLoginHandler", e)

                Log.log(logger::trace, "Got exception in createLoginHandler , returning NoOpLoginHandler", analyticsProvider.apiUrl)
                NoOpLoginHandler("error in createLoginHandler $e")
            }
        }
    }


    fun login(user: String, password: String): LoginResult

    //does login or refresh if necessary, return true if did successful login or successful refresh,
    // or when no need to do anything. return false if failed.
    //we don't really rely on the returned value, mostly for logging
    fun loginOrRefresh(onAuthenticationError: Boolean = false): Boolean


    fun logout(): Boolean {
        return try {
            Log.log(logger::trace, "logout called")

            val digmaAccount = DigmaDefaultAccountHolder.getInstance().account

            Log.log(logger::trace, "logout: found account {}", digmaAccount)

            digmaAccount?.let { account ->
                runBlocking {
                    Log.log(logger::trace, "logout: deleting account {}", account)
                    DigmaAccountManager.getInstance().removeAccount(account)
                    DigmaDefaultAccountHolder.getInstance().account = null
                }
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


}
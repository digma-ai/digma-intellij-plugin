package org.digma.intellij.plugin.auth.account

import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.CoroutineName
import org.digma.intellij.plugin.analytics.ApiErrorHandler
import org.digma.intellij.plugin.analytics.ReplacingClientException
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.auth.NoOpLoginHandler
import org.digma.intellij.plugin.auth.credentials.DigmaCredentials
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import kotlin.coroutines.coroutineContext

interface LoginHandler {

    val logger: Logger

    companion object {

        private val logger: Logger = Logger.getInstance(this::class.java)


        //markConnectionLostOnErrors should be true only in some callers. for example we don't want the auto refresh to mark connection lost,
        fun createLoginHandler(analyticsProvider: RestAnalyticsProvider, markConnectionLostOnErrors: Boolean = false): LoginHandler {

            try {

                Log.log(logger::trace, "createLoginHandler called for url {}", analyticsProvider.apiUrl)

                val loginHandler = when (analyticsProvider.about.isCentralize) {
                    true -> CentralizedLoginHandler(analyticsProvider)
                    false, null -> LocalLoginHandler(analyticsProvider)
                }

                //reset connection status in case it's in no connection mode,
                //resetConnectionLostAndNotifyIfNecessary is a fast method when the connection is ok
                ApiErrorHandler.getInstance().resetConnectionLostAndNotifyIfNecessary(null)

                Log.log(logger::trace, "created {} for url {}", loginHandler, analyticsProvider.apiUrl)
                return loginHandler

            } catch (e: ReplacingClientException) {
                //ignore,its momentary
                return NoOpLoginHandler("error in createLoginHandler $e")
            } catch (e: Throwable) {

                //if we can't create a login handler we assume there is a connection issue, it may be a real connect issue,
                // or any other issue were we can't call analyticsProvider.about
                if (markConnectionLostOnErrors) {
                    ApiErrorHandler.getInstance().handleAuthManagerCantConnectError(e, null)
                }

                Log.warnWithException(logger, e, "Exception in createLoginHandler {}, url {}", e, analyticsProvider.apiUrl)
                ErrorReporter.getInstance().reportError("AuthManager.createLoginHandler", e)

                Log.log(logger::trace, "Got exception in createLoginHandler , returning NoOpLoginHandler", analyticsProvider.apiUrl)
                return NoOpLoginHandler("error in createLoginHandler $e")
            }
        }
    }


    suspend fun login(user: String, password: String): LoginResult

    /**
     * does login or refresh if necessary, return true if did successful login or successful refresh,or did nothing because credentials are valid.
     * false otherwise.
     * don't rely on the result for authentication purposes, its mainly for logging
     */
    suspend fun loginOrRefresh(onAuthenticationError: Boolean = false): Boolean

    /**
     * return true if refresh was successful
     */
    suspend fun refresh(account: DigmaAccount, credentials: DigmaCredentials): Boolean


    /**
     * return true only if an account was really deleted
     */
    suspend fun logout(): Boolean {
        return try {
            trace("logout called")

            val digmaAccount = DigmaDefaultAccountHolder.getInstance().account

            trace("logout: found account {}", digmaAccount)

            digmaAccount?.let { account ->
                SingletonAccountUpdater.deleteAccount(account)
            }

            digmaAccount?.let {
                trace("logout: account deleted {} ", digmaAccount)
            }

            //return true only if an account was really deleted
            digmaAccount != null

        } catch (e: Throwable) {
            warnWithException(e, "Exception in logout {}", e)
            ErrorReporter.getInstance().reportError("AuthManager.logout", e)
            false
        }
    }


    fun reportPosthogEvent(evenName: String, details: Map<String, String> = mapOf()) {
        findActiveProject()?.let { project ->
            ActivityMonitor.getInstance(project).registerCustomEvent(evenName, details)
        }
    }


//    suspend fun warn(format: String, vararg args: Any?) {
//        Log.log(logger::warn, "${coroutineContext[CoroutineName]}: $format", args)
//    }

    suspend fun trace(format: String, vararg args: Any?) {
        Log.log(logger::trace, "${coroutineContext[CoroutineName]}: $format", *args)
    }

    suspend fun warnWithException(e: Throwable, format: String, vararg args: Any?) {
        Log.warnWithException(logger, e, "${coroutineContext[CoroutineName]}: $format", *args)
    }


}
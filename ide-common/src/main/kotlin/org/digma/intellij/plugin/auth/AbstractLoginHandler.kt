package org.digma.intellij.plugin.auth

import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.analytics.AuthenticationException
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.auth.account.DigmaAccount
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log

abstract class AbstractLoginHandler(protected val analyticsProvider: RestAnalyticsProvider) : LoginHandler {

    override val logger: Logger = Logger.getInstance(this::class.java)

    protected val authApiClient = AuthApiClient(analyticsProvider)

    protected fun defaultAccountExists(): Boolean {
        return DigmaDefaultAccountHolder.getInstance().account != null
    }

    protected fun getDefaultAccount(): DigmaAccount? {
        return DigmaDefaultAccountHolder.getInstance().account
    }


    override fun login(user: String, password: String): LoginResult {

        return try {

            reportPosthogEvent("login", mapOf("user" to user))

            Log.log(logger::trace, "login called for url {}, user {}", analyticsProvider.apiUrl, user)

            if (defaultAccountExists()) {
                Log.log(logger::trace, "default account exists, deleting before login, {}", getDefaultAccount())
                logout()
            }

            Log.log(logger::trace, "doing login for url {}, user {}", analyticsProvider.apiUrl, user)

            val loginResult = authApiClient.login(user, password)

            Log.log(logger::trace, "login success for url {}, user {}, created account {}", analyticsProvider.apiUrl, user, getDefaultAccount())

            reportPosthogEvent("login success", mapOf("user" to user))

            loginResult

        } catch (e: Throwable) {

            if (e is AuthenticationException) {
                Log.warnWithException(logger, e, "Exception in login, url {}", analyticsProvider.apiUrl)
                ErrorReporter.getInstance().reportError("AuthManager.login", e)
                val errorMessage = ExceptionUtils.getNonEmptyMessage(e)
                reportPosthogEvent("login failed", mapOf("user" to user, "error" to errorMessage.toString()))
                //return no success LoginResult
                LoginResult(false, null, e.detailedMessage)
            }

            //don't report connection errors, there is no point, backend may be down and that happens a lot
            if (!ExceptionUtils.isAnyConnectionException(e)) {
                Log.warnWithException(logger, e, "Exception in login, url {}", analyticsProvider.apiUrl)
                ErrorReporter.getInstance().reportError("AuthManager.login", e)
            }

            LoginResult(false, null, ExceptionUtils.getNonEmptyMessage(e))

        }
    }

}
package org.digma.intellij.plugin.auth.account

import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.analytics.AuthenticationException
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.auth.AuthApiClient
import org.digma.intellij.plugin.auth.credentials.DigmaCredentials
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.errorreporting.ErrorReporter

abstract class AbstractLoginHandler(protected val analyticsProvider: RestAnalyticsProvider) : LoginHandler {

    override val logger: Logger = Logger.getInstance(this::class.java)

    private val authApiClient = AuthApiClient(analyticsProvider)

    private fun defaultAccountExists(): Boolean {
        return DigmaDefaultAccountHolder.getInstance().account != null
    }

    protected fun getDefaultAccount(): DigmaAccount? {
        return DigmaDefaultAccountHolder.getInstance().account
    }


    override suspend fun login(user: String, password: String): LoginResult {

        return try {

            reportPosthogEvent("login", mapOf("user" to user))

            trace("login called for url {}, user {}", analyticsProvider.apiUrl, user)

            if (defaultAccountExists()) {
                trace("default account exists, deleting before login, {}", getDefaultAccount())
                logout()
            }

            trace("doing login for url {}, user {}", analyticsProvider.apiUrl, user)

            val credentials = authApiClient.login(user, password)

            val account = DigmaAccountManager.createAccount(analyticsProvider.apiUrl, credentials.userId)
            updateAccount(account, credentials)

            trace("login success for url {}, user {}, created account {}", analyticsProvider.apiUrl, user, getDefaultAccount())

            reportPosthogEvent("login success", mapOf("user" to user))

            LoginResult(true, credentials.userId, null)

        } catch (e: Throwable) {

            warnWithException(e, "Exception in login {}, url {}", e, analyticsProvider.apiUrl)
            ErrorReporter.getInstance().reportError("AuthManager.login", e)

            if (e is AuthenticationException) {
                warnWithException(e, "Exception in login, url {}", analyticsProvider.apiUrl)
                ErrorReporter.getInstance().reportError("AuthManager.login", e)
                val errorMessage = ExceptionUtils.getNonEmptyMessage(e)
                reportPosthogEvent("login failed", mapOf("user" to user, "error" to errorMessage))
                //return no success LoginResult
                LoginResult(false, null, e.detailedMessage)
            }

            LoginResult(false, null, ExceptionUtils.getNonEmptyMessage(e))
        }
    }


    override suspend fun refresh(account: DigmaAccount, credentials: DigmaCredentials): Boolean {

        return try {

            trace("refresh called for url {}", analyticsProvider.apiUrl)

            val newCredentials = authApiClient.refreshToken(account, credentials)
            updateAccount(account, newCredentials)

            trace("refresh success for url {}, updated account {}", analyticsProvider.apiUrl, getDefaultAccount())

            true

        } catch (e: Throwable) {
            warnWithException(e, "Exception in refresh {}", e)
            ErrorReporter.getInstance().reportError("AuthManager.refresh", e)

            if (e is AuthenticationException) {
                warnWithException(e, "Exception in refresh, url {}", analyticsProvider.apiUrl)
                ErrorReporter.getInstance().reportError("AuthManager.refresh", e)
                val errorMessage = ExceptionUtils.getNonEmptyMessage(e)
                reportPosthogEvent("refresh failed", mapOf("error" to errorMessage))
            }

            throw e
        }
    }





}
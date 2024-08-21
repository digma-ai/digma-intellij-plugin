package org.digma.intellij.plugin.auth.account

import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.CoroutineName
import org.digma.intellij.plugin.analytics.AuthenticationException
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.auth.AuthApiClient
import org.digma.intellij.plugin.auth.credentials.DigmaCredentials
import org.digma.intellij.plugin.auth.reportAuthPosthogEvent
import org.digma.intellij.plugin.auth.withAuthManagerDebug
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import kotlin.coroutines.coroutineContext

abstract class AbstractLoginHandler(protected val analyticsProvider: RestAnalyticsProvider) : LoginHandler {

    override val logger: Logger = Logger.getInstance(this::class.java)

    private val authApiClient = AuthApiClient(analyticsProvider)

    private fun defaultAccountExists(): Boolean {
        return DigmaDefaultAccountHolder.getInstance().account != null
    }

    protected fun getDefaultAccount(): DigmaAccount? {
        return DigmaDefaultAccountHolder.getInstance().account
    }


    override suspend fun login(user: String, password: String, trigger: String): LoginResult {

        return try {

            reportAuthPosthogEvent("login", this.javaClass.simpleName, trigger, mapOf("user" to user))

            trace("login called for url {}, user {},trigger {}", analyticsProvider.apiUrl, user, trigger)

            if (defaultAccountExists()) {
                trace("default account exists, deleting before login, {}", getDefaultAccount())
                logout("${this.javaClass.simpleName}: ensure logout before login")
            }

            trace("calling login for url {}, user {},trigger {}", analyticsProvider.apiUrl, user, trigger)

            val credentials = authApiClient.login(user, password)

            trace("creating new account for {}", analyticsProvider.apiUrl)

            val account = DigmaAccountManager.createAccount(
                analyticsProvider.apiUrl,
                credentials.userId,
                coroutineContext[CoroutineName].toString(),
                java.time.Instant.now().toString()
            )

            trace("new account created for {}, {}", analyticsProvider.apiUrl, account)

            SingletonAccountUpdater.updateNewAccount(account, credentials)

            trace("login success for url {}, user {}, created account {},trigger {}", analyticsProvider.apiUrl, user, getDefaultAccount(), trigger)

            reportAuthPosthogEvent(
                "login success", this.javaClass.simpleName, trigger, mapOf(
                    "user" to user,
                    "account.id" to account.id,
                    "accessTokenHash" to credentials.accessTokenHash(),
                    "refreshTokenHash" to credentials.refreshTokenHash(),
                    "expirationDate" to credentials.getExpirationTimeAsDate()
                )
            )

            LoginResult(true, credentials.userId, null)

        } catch (e: Throwable) {

            warnWithException(e, "Exception in login {}, url {}", e, analyticsProvider.apiUrl)
            ErrorReporter.getInstance().reportError("${javaClass.simpleName}.login", e, mapOf("login trigger" to trigger))
            val errorMessage = ExceptionUtils.getNonEmptyMessage(e)
            reportAuthPosthogEvent(
                "login failed",
                this.javaClass.simpleName,
                trigger,
                mapOf("user" to user, "error" to errorMessage)
            )

            LoginResult(false, null, ExceptionUtils.getNonEmptyMessage(e))
        }
    }


    override suspend fun refresh(account: DigmaAccount, credentials: DigmaCredentials, trigger: String): Boolean {

        return try {

            trace("refresh called for url {},trigger {}", analyticsProvider.apiUrl, trigger)

            withAuthManagerDebug {
                reportAuthPosthogEvent(
                    "refresh token", this.javaClass.simpleName, trigger, mapOf(
                        "account.id" to account.id,
                        "accessTokenHash" to credentials.accessTokenHash(),
                        "refreshTokenHash" to credentials.refreshTokenHash(),
                        "expirationDate" to credentials.getExpirationTimeAsDate()
                    )
                )
            }

            val newCredentials = authApiClient.refreshToken(account, credentials)
            SingletonAccountUpdater.updateAccount(account, newCredentials)

            trace("refresh success for url {}, updated account {},trigger {}", analyticsProvider.apiUrl, getDefaultAccount(), trigger)

            withAuthManagerDebug {
                reportAuthPosthogEvent(
                    "refresh token success", this.javaClass.simpleName, trigger, mapOf(
                        "account.id" to account.id,
                        "oldAccessTokenHash" to credentials.accessTokenHash(),
                        "oldRefreshTokenHash" to credentials.refreshTokenHash(),
                        "oldExpirationDate" to credentials.getExpirationTimeAsDate(),
                        "newAccessTokenHash" to newCredentials.accessTokenHash(),
                        "newRefreshTokenHash" to newCredentials.refreshTokenHash(),
                        "newExpirationDate" to newCredentials.getExpirationTimeAsDate()
                    )
                )
            }

            true

        } catch (e: Throwable) {
            warnWithException(e, "Exception in refresh {}", e)
            ErrorReporter.getInstance().reportError("${javaClass.simpleName}.refresh", e, mapOf("refresh trigger" to trigger))
            val errorMessage = ExceptionUtils.getNonEmptyMessage(e)
            reportAuthPosthogEvent(
                "refresh token failed", this.javaClass.simpleName, trigger, mapOf(
                    "error" to errorMessage,
                    "account.id" to account.id,
                    "accessTokenHash" to credentials.accessTokenHash(),
                    "refreshTokenHash" to credentials.refreshTokenHash(),
                    "expirationDate" to credentials.getExpirationTimeAsDate()
                )
            )

            val authenticationException = ExceptionUtils.findCause(AuthenticationException::class.java, e)
            if (authenticationException != null) {
                throw e
            } else {
                false
            }
        }
    }


}
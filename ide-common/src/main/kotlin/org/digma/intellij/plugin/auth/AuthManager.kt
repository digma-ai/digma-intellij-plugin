package org.digma.intellij.plugin.auth

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.runBlocking
import org.digma.intellij.plugin.analytics.AnalyticsProvider
import org.digma.intellij.plugin.analytics.AnalyticsProviderException
import org.digma.intellij.plugin.analytics.AuthenticationException
import org.digma.intellij.plugin.analytics.AuthenticationProvider
import org.digma.intellij.plugin.auth.account.DigmaAccount
import org.digma.intellij.plugin.auth.account.DigmaAccountManager
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
import org.digma.intellij.plugin.auth.credentials.DigmaCredentials
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.model.rest.login.LoginRequest
import org.digma.intellij.plugin.model.rest.login.RefreshRequest
import org.digma.intellij.plugin.persistence.PersistenceService
import java.io.Closeable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy


@Service(Service.Level.APP)
class AuthManager : AuthenticationProvider {

    private var myTokenProvider: TokenProvider? = null

    companion object {
        @JvmStatic
        fun getInstance(): AuthManager {
            return service<AuthManager>()
        }
    }

    override fun getAuthenticationToken(): String? {
        return myTokenProvider?.provideToken()
    }

    fun withAuth(analyticsProvider: AnalyticsProvider, url: String): AnalyticsProvider {

        loginOrRefresh(analyticsProvider, url)

        //always return a proxy. even if login failed. the proxy will try to log in or refresh on authentication error
        return proxy(analyticsProvider, url)
    }


    private fun loginOrRefresh(analyticsProvider: AnalyticsProvider, url: String) {

        try {

            val digmaAccount = DigmaDefaultAccountHolder.getInstance().account

            val about = analyticsProvider.about
            if (about.isCentralize == true) {
                myTokenProvider = SettingsTokenProvider()
                digmaAccount?.let { account ->
                    runBlocking {
                        DigmaAccountManager.getInstance().removeAccount(account)
                        DigmaDefaultAccountHolder.getInstance().account = null
                    }
                }
            } else {

                if (digmaAccount == null) {
                    login(analyticsProvider, url)
                } else {
                    val credentials = runBlocking {
                        DigmaAccountManager.getInstance().findCredentials(digmaAccount)
                    }
                    if (credentials == null) {
                        runBlocking {
                            DigmaAccountManager.getInstance().removeAccount(digmaAccount)
                        }
                        DigmaDefaultAccountHolder.getInstance().account = null
                        login(analyticsProvider, url)
                    } else {
                        refresh(analyticsProvider, digmaAccount, credentials, url)
                    }
                }
                myTokenProvider = DefaultAccountTokenProvider()
            }
        } catch (e: AnalyticsProviderException) {
            ErrorReporter.getInstance().reportError("AuthManager.withAuth", e)
        }
    }


    private fun login(analyticsProvider: AnalyticsProvider, url: String) {
        val email = PersistenceService.getInstance().getUserRegistrationEmail() ?: PersistenceService.getInstance().getUserEmail()
        val loginResponse = analyticsProvider.login(LoginRequest("admin@digma.ai", email, "admin"))
        val digmaAccount = DigmaAccountManager.createAccount(url)
        val expireIn = loginResponse.expiration.time - System.currentTimeMillis()
        val digmaCredentials = DigmaCredentials(
            loginResponse.accessToken,
            loginResponse.refreshToken,
            expireIn,
            url,
            TokenType.Bearer.name,
            loginResponse.expiration.time
        )
        runBlocking {
            DigmaAccountManager.getInstance().updateAccount(digmaAccount, digmaCredentials)
        }
        DigmaDefaultAccountHolder.getInstance().account = digmaAccount
    }


    private fun refresh(analyticsProvider: AnalyticsProvider, digmaAccount: DigmaAccount, credentials: DigmaCredentials, url: String) {

        val loginResponse = analyticsProvider.refreshToken(RefreshRequest(credentials.accessToken, credentials.refreshToken))
        val expireIn = loginResponse.expiration.time - System.currentTimeMillis()
        val digmaCredentials = DigmaCredentials(
            loginResponse.accessToken,
            loginResponse.refreshToken,
            expireIn,
            url,
            TokenType.Bearer.name,
            loginResponse.expiration.time
        )
        runBlocking {
            DigmaAccountManager.getInstance().updateAccount(digmaAccount, digmaCredentials)
        }
        DigmaDefaultAccountHolder.getInstance().account = digmaAccount
    }


    private fun proxy(analyticsProvider: AnalyticsProvider, url: String): AnalyticsProvider {
        return Proxy.newProxyInstance(
            this::class.java.classLoader,
            arrayOf(AnalyticsProvider::class.java, Closeable::class.java),
            MyAuthInvocationHandler(analyticsProvider, url)
        ) as AnalyticsProvider
    }


    inner class MyAuthInvocationHandler(private val analyticsProvider: AnalyticsProvider, private val url: String) : InvocationHandler {

        override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
            return try {
                if (args == null) {
                    method.invoke(analyticsProvider)
                } else {
                    method.invoke(analyticsProvider, *args)
                }

            } catch (e: AuthenticationException) {
                loginOrRefresh(analyticsProvider, url)
                if (args == null) {
                    method.invoke(analyticsProvider)
                } else {
                    method.invoke(analyticsProvider, *args)
                }
            }
        }
    }
}
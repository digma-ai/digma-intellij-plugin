package org.digma.intellij.plugin.auth

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.runBlocking
import org.digma.intellij.plugin.analytics.AnalyticsProvider
import org.digma.intellij.plugin.analytics.AuthenticationException
import org.digma.intellij.plugin.analytics.AuthenticationProvider
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.auth.account.DigmaAccount
import org.digma.intellij.plugin.auth.account.DigmaAccountManager
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
import org.digma.intellij.plugin.auth.credentials.DigmaCredentials
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.login.LoginRequest
import org.digma.intellij.plugin.model.rest.login.RefreshRequest
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.io.Closeable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock


const val SILENT_LOGIN_USER = "admin@digma.ai"
const val SILENT_LOGIN_PASSWORD = "admin!"

data class LoginResult(val isSuccess: Boolean, val userId: String?, val error: String?)

@Service(Service.Level.APP)
class AuthManager {

    private val logger: Logger = Logger.getInstance(AuthManager::class.java)
    private val cud = CUD()

    private val listeners: MutableList<AuthInfoChangeListener> = ArrayList()
    private var analyticsProvider: RestAnalyticsProvider? = null

    private val loginOrRefreshLock = ReentrantLock()
    private val isProxyPaused: AtomicBoolean = AtomicBoolean(true)

    companion object {
        @JvmStatic
        fun getInstance(): AuthManager {
            return service<AuthManager>()
        }
    }


    fun getAuthenticationProviders(): List<AuthenticationProvider> {
        return listOf(
            DigmaAccessTokenAuthenticationProvider(SettingsTokenProvider()),
            HeaderTokenAuthenticationProvider(DefaultAccountTokenProvider())
        )
    }

    fun addAuthInfoChangeListener(listener: AuthInfoChangeListener, parentDisposable: Disposable) {
        listeners.add(listener)

        Disposer.register(parentDisposable) { removeAuthInfoChangeListener(listener) }
    }

    fun removeAuthInfoChangeListener(listener: AuthInfoChangeListener) {
        listeners.remove(listener)
    }

    //withAuth should not be called concurrently by multiple threads. it is called only from AnalyticsService.replaceClient.
    //AnalyticsService.replaceClient is called on startup and when a relevant settings change and is synchronized.
    //the analyticsProvider here will usually be a non proxied RestAnalyticsProvider
    fun withAuth(analyticsProvider: RestAnalyticsProvider): AnalyticsProvider {

        Log.log(logger::trace, "wrapping analyticsProvider with auth for url={}", analyticsProvider.apiUrl)

        //keep the RestAnalyticsProvider every time withAuth is called, so it will always be the correct and latest one
        this.analyticsProvider = analyticsProvider

        //loginOrRefresh will fail if backend is not up. it should catch all possible exceptions so that a proxy will always be returned.
        loginOrRefresh()


        //always return a proxy. even if login failed. the proxy will try to loginOrRefresh on AuthenticationException
        val proxy = proxy(analyticsProvider)

        Log.log(
            logger::trace, "analyticsProvider wrapped with auth proxy, current account {},analytics url {}",
            DigmaDefaultAccountHolder.getInstance().account,
            analyticsProvider.apiUrl
        )

        Log.log(logger::info, "resuming current proxy, analytics url {}", analyticsProvider?.apiUrl)
        isProxyPaused.set(false)
        return proxy
    }


    fun login(user: String, password: String): LoginResult {
        val loginResult = analyticsProvider?.let {
            cud.login(it, user, password)
        } ?: LoginResult(false, null, "analyticsProvider is null")

        fireChange()
        return loginResult
    }


    fun logout() {
        cud.logoutDefaultAccount()
        fireChange()
    }


    private fun loginOrRefresh(forceRefresh: Boolean = false): Boolean {

        val localAnalyticsProvider = analyticsProvider ?: return false

        //loginOrRefresh should never throw exception but return true or false
        //it may that that few threads will fail on AuthenticationException so make sure not to login or refresh concurrently
        //accountLock.lock()
        try {

            loginOrRefreshLock.lock()

            Log.log(logger::trace, "loginOrRefresh called, url={}", localAnalyticsProvider.apiUrl)

            val digmaAccount = DigmaDefaultAccountHolder.getInstance().account
            val isCentralize = localAnalyticsProvider.about.isCentralize ?: false

            //centralized deployment, don't log in if account is null, user will sign up
            if (digmaAccount == null && isCentralize) {
                Log.log(logger::trace, "loginOrRefresh, account is not logged in for centralized env url={}", localAnalyticsProvider.apiUrl)
                return false
            }


            //if digma account is null do silent log in, it's not centralized,we checked above
            if (digmaAccount == null) {
                Log.log(logger::trace, "digma account is null and not centralized,doing silent login for url {}", localAnalyticsProvider.apiUrl)
                val loginResult = cud.login(localAnalyticsProvider, SILENT_LOGIN_USER, SILENT_LOGIN_PASSWORD)
                return loginResult.isSuccess
            }


            if (digmaAccount.server.url != localAnalyticsProvider.apiUrl) {
                Log.log(
                    logger::trace, "digma account exists,but its url is different then analytics url, deleting account {}, url {}",
                    digmaAccount,
                    localAnalyticsProvider.apiUrl
                )
                cud.logoutDefaultAccount()
                if (!isCentralize) {
                    Log.log(logger::trace, "not centralized,doing silent login for url {}", localAnalyticsProvider.apiUrl)
                    val loginResult = cud.login(localAnalyticsProvider, SILENT_LOGIN_USER, SILENT_LOGIN_PASSWORD)
                    return loginResult.isSuccess
                } else {
                    Log.log(logger::trace, "default account deleted and is centralized, skipping login for url {}", localAnalyticsProvider.apiUrl)
                    return false
                }
            }

            //digma account is not null, either it's loaded from persistence on startup or token expired
            Log.log(logger::trace, "digma account exists,trying to refresh if necessary, url {}", localAnalyticsProvider.apiUrl)
            val result = doRefresh(localAnalyticsProvider, digmaAccount, isCentralize, forceRefresh)
            Log.log(logger::trace, "completed loginOrRefresh for url {}", localAnalyticsProvider.apiUrl)
            return result

        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "Exception in loginOrRefresh {}", e)
            ErrorReporter.getInstance().reportInternalFatalError("AuthManager.loginOrRefresh", e)
            return false
        } finally {
            if (loginOrRefreshLock.isHeldByCurrentThread) {
                loginOrRefreshLock.unlock()
            }
            fireChange()
        }
    }


    //this method may throw exceptions, always catch exception when calling it.
    // it is meant just to make more readable code, but it is part of loginOrRefresh and should not be called
    // from other code
    private fun doRefresh(
        localAnalyticsProvider: RestAnalyticsProvider,
        digmaAccount: DigmaAccount,
        isCentralize: Boolean,
        forceRefresh: Boolean
    ): Boolean {

        val credentials = runBlocking {
            DigmaAccountManager.getInstance().findCredentials(digmaAccount)
        }

        //if digma account is not null and credentials is null then probably something corrupted,
        // it may be that the credentials deleted from the password safe
        if (credentials == null) {
            Log.log(
                logger::warn,
                "no credentials found for account {} maybe credentials deleted from password safe? deleting account, analytics url {}",
                digmaAccount,
                localAnalyticsProvider.apiUrl
            )
            //logout to delete the account
            cud.logoutDefaultAccount()
            //do silent login if centralized, otherwise skip and user will have to sign up again
            if (!isCentralize) {
                Log.log(
                    logger::warn,
                    "no credentials found for account {} and its not centralized,doing silent login, analytics url {}",
                    digmaAccount,
                    localAnalyticsProvider.apiUrl
                )
                val loginResult = cud.login(localAnalyticsProvider, SILENT_LOGIN_USER, SILENT_LOGIN_PASSWORD)
                return loginResult.isSuccess
            } else {
                Log.log(
                    logger::warn,
                    "no credentials found for account {} and its centralized,skipping login, analytics url {}",
                    digmaAccount,
                    localAnalyticsProvider.apiUrl
                )
                return false
            }
        }

        if (!credentials.isAccessTokenValid() || forceRefresh) {
            val because = if (!credentials.isAccessTokenValid()) "access token expired" else "called with force refresh"
            Log.log(
                logger::trace,
                "loginOrRefresh, need to refresh credentials for account {} because {},credentials {}, url={}",
                digmaAccount,
                because,
                credentials,
                localAnalyticsProvider.apiUrl
            )

            if (cud.refreshToken(localAnalyticsProvider, digmaAccount, credentials, localAnalyticsProvider.apiUrl)) {
                Log.log(
                    logger::trace,
                    "loginOrRefresh, token refreshed",
                    digmaAccount,
                    localAnalyticsProvider.apiUrl,
                    credentials
                )
                return true
            } else if (!isCentralize) {
                Log.log(
                    logger::trace,
                    "refreshToken failed and its not centralized, doing silent login for url {}", localAnalyticsProvider.apiUrl
                )
                cud.logoutDefaultAccount()
                val loginResult = cud.login(localAnalyticsProvider, SILENT_LOGIN_USER, SILENT_LOGIN_PASSWORD)
                return loginResult.isSuccess
            } else {
                Log.log(
                    logger::trace,
                    "refreshToken didn't complete successfully and its centralized, skipping login for url {}",
                    localAnalyticsProvider.apiUrl
                )
                return false
            }
        } else {
            Log.log(
                logger::trace,
                "no need to refresh token for account {}, analytics url {}",
                digmaAccount,
                localAnalyticsProvider.apiUrl
            )
        }

        return true
    }


    private fun fireChange() {

        Log.log(
            logger::trace, "firing authInfoChanged, default account {}, analytics url {}",
            DigmaDefaultAccountHolder.getInstance().account,
            analyticsProvider?.apiUrl
        )

        val authInfo = AuthInfo(DigmaDefaultAccountHolder.getInstance().account?.userId)
        listeners.forEach {
            try {
                it.authInfoChanged(authInfo)
            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportInternalFatalError("Failed notify auth info change", e)
            }
        }
    }


    fun pauseCurrentProxy() {
        Log.log(logger::info, "pausing current proxy, analytics url {}", analyticsProvider?.apiUrl)
        analyticsProvider = null
        isProxyPaused.set(true)
    }


    private fun proxy(analyticsProvider: RestAnalyticsProvider): AnalyticsProvider {
        return Proxy.newProxyInstance(
            this::class.java.classLoader,
            arrayOf(AnalyticsProvider::class.java, Closeable::class.java),
            MyAuthInvocationHandler(analyticsProvider)
        ) as AnalyticsProvider
    }


    private fun reportPosthogEvent(evenName: String, details: Map<String, String> = mapOf()) {
        findActiveProject()?.let { project ->
            ActivityMonitor.getInstance(project).registerCustomEvent(evenName, details)
        }
    }


    //like sql CRUD: create,read,update,delete, we don't have read so CUD
    //basic building blocks for AuthManager: login, logout and refresh
    private inner class CUD {

        private val accountLock = ReentrantLock()


        fun login(analyticsProvider: RestAnalyticsProvider, userName: String, password: String): LoginResult {

            Log.log(logger::trace, "login called for url {}, user:{}", analyticsProvider.apiUrl, userName)

            return try {
                accountLock.lock()

                reportPosthogEvent("login", mapOf("user" to userName))

                Log.log(logger::trace, "doing login for url {}, user:{}", analyticsProvider.apiUrl, userName)
                val loginResponse = analyticsProvider.login(LoginRequest(userName, password))

                val digmaAccount = DigmaAccountManager.createAccount(analyticsProvider.apiUrl, loginResponse.userId)
                val digmaCredentials = DigmaCredentials(
                    loginResponse.accessToken,
                    loginResponse.refreshToken,
                    analyticsProvider.apiUrl,
                    TokenType.Bearer.name,
                    loginResponse.expiration.time
                )
                runBlocking {
                    DigmaAccountManager.getInstance().updateAccount(digmaAccount, digmaCredentials)
                    DigmaDefaultAccountHolder.getInstance().account = digmaAccount
                }
                Log.log(logger::trace, "login success for url={}, user:{}, created account {}", analyticsProvider.apiUrl, userName, digmaAccount)

                reportPosthogEvent("login success", mapOf("user" to SILENT_LOGIN_USER))

                LoginResult(true, loginResponse.userId, null)

            } catch (e: Throwable) {
                Log.debugWithException(logger, e, "login failed {}", e)
                ErrorReporter.getInstance().reportInternalFatalError("AuthManager.login", e)
                val errorMessage = ExceptionUtils.getNonEmptyMessage(e)
                reportPosthogEvent("login failed", mapOf("user" to SILENT_LOGIN_USER, "error" to errorMessage.toString()))
                LoginResult(false, null, e.toString())
            } finally {
                if (accountLock.isHeldByCurrentThread) {
                    accountLock.unlock()
                }
            }
        }


        fun logoutDefaultAccount(): Boolean {

            return try {

                Log.log(logger::trace, "logout called , analytics url {}", analyticsProvider?.apiUrl)

                accountLock.lock()

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
                    Log.log(logger::trace, "logout: account deleted {} , analytics url {}", digmaAccount, analyticsProvider?.apiUrl)
                }

                true
            } catch (e: Throwable) {
                Log.debugWithException(logger, e, "logout failed, analytics url {}, {}", analyticsProvider?.apiUrl, e)
                ErrorReporter.getInstance().reportError("AuthManager.logout", e)
                false
            } finally {
                if (accountLock.isHeldByCurrentThread) {
                    accountLock.unlock()
                }
            }
        }


        fun refreshToken(
            analyticsProvider: RestAnalyticsProvider,
            digmaAccount: DigmaAccount,
            credentials: DigmaCredentials,
            url: String
        ): Boolean {

            Log.log(logger::trace, "refreshToken called for account {}, analytics url {}", digmaAccount, url)

            return try {
                accountLock.lock()

                Log.log(logger::trace, "refreshing credentials for account {}, analytics url {}", digmaAccount, url)
                val loginResponse = analyticsProvider.refreshToken(RefreshRequest(credentials.accessToken, credentials.refreshToken))
                val digmaCredentials = DigmaCredentials(
                    loginResponse.accessToken,
                    loginResponse.refreshToken,
                    url,
                    TokenType.Bearer.name,
                    loginResponse.expiration.time
                )
                runBlocking {
                    DigmaAccountManager.getInstance().updateAccount(digmaAccount, digmaCredentials)
                    DigmaDefaultAccountHolder.getInstance().account = digmaAccount
                }
                Log.log(logger::trace, "refresh token success for account {}, analytics url {}", digmaAccount, url)

                true

            } catch (e: AuthenticationException) {
                Log.debugWithException(logger, e, "AuthenticationException in refresh for account {}, analytics url {},{}", digmaAccount, url, e)
                ErrorReporter.getInstance().reportInternalFatalError("AuthManager.refresh", e)

                false
            } finally {
                if (accountLock.isHeldByCurrentThread) {
                    accountLock.unlock()
                }
            }
        }


    }


    private inner class MyAuthInvocationHandler(private val analyticsProvider: RestAnalyticsProvider) : InvocationHandler {

        override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
            return try {
                if (args == null) {
                    method.invoke(analyticsProvider)
                } else {
                    method.invoke(analyticsProvider, *args)
                }

            } catch (e: InvocationTargetException) {

                Log.debugWithException(logger, e, "Exception in proxy {}", ExceptionUtils.getNonEmptyMessage(e))
                if (isProxyPaused.get()) {
                    Log.log(logger::trace, "got Exception in proxy but proxy is paused, rethrowing")
                    throw e
                }

                //check if this is an AuthenticationException, if not rethrow the exception
                @Suppress("UNUSED_VARIABLE")
                val authenticationException = ExceptionUtils.find(e, AuthenticationException::class.java)
                    ?: throw e

                Log.log(logger::trace, "got AuthenticationException, calling loginOrRefresh")
                //if loginOrRefresh fails here there is no point in calling the method again.
                // it will probably fail again and will cause an endless loop.
                //just wait for the next invocation to try login or refresh again.
                if (loginOrRefresh(true)) {
                    Log.log(
                        logger::trace, "loginOrRefresh success after AuthenticationException, current account {},analytics url {}",
                        DigmaDefaultAccountHolder.getInstance().account,
                        analyticsProvider.apiUrl
                    )

                    if (args == null) {
                        method.invoke(analyticsProvider)
                    } else {
                        method.invoke(analyticsProvider, *args)
                    }
                } else {
                    Log.log(
                        logger::trace,
                        "loginOrRefresh failed after AuthenticationException, rethrowing exception {}",
                        ExceptionUtils.getNonEmptyMessage(e)
                    )
                    //must return a value ot throw the exception
                    throw e
                }
            }
        }
    }
}
package org.digma.intellij.plugin.auth

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.runBlocking
import org.digma.intellij.plugin.analytics.AnalyticsProvider
import org.digma.intellij.plugin.analytics.AnalyticsProviderException
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
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.io.Closeable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.locks.ReentrantLock


const val SILENT_LOGIN_USER = "admin@digma.ai"
const val SILENT_LOGIN_PASSWORD = "admin"

@Service(Service.Level.APP)
class AuthManager {

    private val logger: Logger = Logger.getInstance(AuthManager::class.java)

    private val loginOrRefreshLock = ReentrantLock()

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
        listeners.add(listener);

        Disposer.register(parentDisposable) { removeAuthInfoChangeListener(listener) }
    }

    fun removeAuthInfoChangeListener(listener: AuthInfoChangeListener) {
        listeners.remove(listener)
    }

    //withAuth should not be called concurrently by multiple threads. it is called only from AnalyticsService.replaceClient.
    //AnalyticsService.replaceClient is called on startup and when a relevant settings change and is synchronized.
    //the analyticsProvider here will usually be a non proxied RestAnalyticsProvider
    fun withAuth(analyticsProvider: RestAnalyticsProvider, url: String): AnalyticsProvider {

        Log.log(logger::info, "wrapping analyticsProvider with auth for url={}", url)

        //loginOrRefresh will fail if backend is not up. it should catch all possible exceptions so that a proxy will always be returned.
        loginOrRefresh(analyticsProvider, url)

        //always return a proxy. even if login failed. the proxy will try to loginOrRefresh on AuthenticationException
        return proxy(analyticsProvider, url)
    }


    //why forceRefresh: theoretically we can rely on credentials.isAccessTokenValid to decide if to refresh.
    // but it's a week decision because we don't know what the server is doing and if the server really considers expiration time.
    // so forceRefresh is sent on AuthenticationException and if we have credentials we try to refresh ignoring credentials.isAccessTokenValid
    private fun loginOrRefresh(analyticsProvider: RestAnalyticsProvider, url: String, forceRefresh: Boolean = false): Boolean {

        //loginOrRefresh should never throw exception but return true or false

        //it may that that few threads will fail on AuthenticationException so make sure not to login or refresh concurrently
        loginOrRefreshLock.lock()
        return try {

            Log.log(logger::info, "loginOrRefresh called, url={}", url)


            val digmaAccount = DigmaDefaultAccountHolder.getInstance().account

            //currently we always do local login

//            val about = analyticsProvider.about
//            if (about.isCentralize == true) {
//                digmaAccount?.let { account ->
//                    runBlocking {
//                        DigmaAccountManager.getInstance().removeAccount(account)
//                        DigmaDefaultAccountHolder.getInstance().account = null
//                    }
//                }
//            } else {

            if (digmaAccount == null) {
                Log.log(logger::info, "loginOrRefresh, account is null,doing login url={}", url)
                login(analyticsProvider, url)
            } else {
                Log.log(logger::info, "loginOrRefresh for account {}, url={}", digmaAccount.name, url)
                val credentials = runBlocking {
                    DigmaAccountManager.getInstance().findCredentials(digmaAccount)
                }
                if (credentials == null) {
                    Log.log(
                        logger::warn,
                        "loginOrRefresh, no credentials found for account {} maybe credentials deleted from password safe? doing new login, url={}",
                        digmaAccount.name,
                        url
                    )
                    runBlocking {
                        DigmaAccountManager.getInstance().removeAccount(digmaAccount)
                        DigmaDefaultAccountHolder.getInstance().account = null
                    }.also {
                        login(analyticsProvider, url)
                    }
                } else if (!credentials.isAccessTokenValid() || forceRefresh) {
                    val because = if (!credentials.isAccessTokenValid()) "access token expired" else "called with force refresh"
                    Log.log(
                        logger::info,
                        "loginOrRefresh, need to refresh credentials for account {} because {},credentials {}, url={}",
                        digmaAccount.name,
                        because,
                        credentials,
                        url
                    )
                    refreshToken(analyticsProvider, digmaAccount, credentials, url)
                } else {
                    Log.log(logger::info, "loginOrRefresh, got account {} with valid credentials {}, url={}", digmaAccount.name, url, credentials)
                }
            }
//            }
            Log.log(logger::info, "loginOrRefresh success, url={}", url)
            true
        } catch (e: AuthenticationException) {
            Log.warnWithException(logger, e, "AuthenticationException in loginOrRefresh {}", e)
            //should never get AuthenticationException because all the apis used here are not protected
            //so report it as reportInternalFatalError
            ErrorReporter.getInstance().reportInternalFatalError("AuthManager.loginOrRefresh", e)
            false
        } catch (e: AnalyticsProviderException) {
            Log.warnWithException(logger, e, "AnalyticsProviderException in loginOrRefresh {}", e)
            val isConnectionException = ExceptionUtils.isAnyConnectionException(e)
            ErrorReporter.getInstance().reportAnalyticsServiceError("AuthManager.loginOrRefresh", "loginOrRefresh", e, isConnectionException)
            false
        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "Exception in loginOrRefresh {}", e)
            ErrorReporter.getInstance().reportInternalFatalError("AuthManager.loginOrRefresh", e)
            false
        } finally {
            if (loginOrRefreshLock.isHeldByCurrentThread) {
                loginOrRefreshLock.unlock()
            }
        }
    }


    private fun login(analyticsProvider: RestAnalyticsProvider, url: String) {

        reportPosthogEvent(
            "login", mapOf(
                "user" to SILENT_LOGIN_USER
            )
        )

        try {
            Log.log(logger::info, "doing login for url={}", url)
            val email = PersistenceService.getInstance().getUserRegistrationEmail() ?: PersistenceService.getInstance().getUserEmail()
            val loginResponse = analyticsProvider.login(LoginRequest(SILENT_LOGIN_USER, email, SILENT_LOGIN_PASSWORD))
            val digmaAccount = DigmaAccountManager.createAccount(url, loginResponse.userId)
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
            Log.log(logger::info, "login success for url={}", url)

            reportPosthogEvent(
                "login success", mapOf(
                    "user" to SILENT_LOGIN_USER
                )
            )

        } catch (e: Throwable) {
            //just for reporting, never swallow the exception
            reportPosthogEvent(
                "login failed", mapOf(
                    "user" to SILENT_LOGIN_USER,
                    "error" to e.message.toString()
                )
            )
            throw e
        }
    }


    private fun refreshToken(analyticsProvider: RestAnalyticsProvider, digmaAccount: DigmaAccount, credentials: DigmaCredentials, url: String) {

        try {
            Log.log(logger::info, "refreshing credentials for account {}, url={}", digmaAccount.name, url)
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
            Log.log(logger::info, "refresh token success for url={}", url)
        } catch (e: AuthenticationException) {
            Log.warnWithException(logger, e, "AuthenticationException in refresh")
            ErrorReporter.getInstance().reportInternalFatalError("AuthManager.refresh", e)

            //usually refresh should not fail on AuthenticationException.
            //but,if refresh fails on AuthenticationException there is probably some corruption with the refresh token
            // or some other server issue that caused it. in that case delete the account and login again hopefully it will succeed.
            Log.log(logger::info, "refresh token failed on AuthenticationException, trying login again. account {}, url={}", digmaAccount.name, url)
            runBlocking {
                DigmaAccountManager.getInstance().removeAccount(digmaAccount)
                DigmaDefaultAccountHolder.getInstance().account = null
            }.also {
                login(analyticsProvider, url)
            }
        }
    }


    private fun proxy(analyticsProvider: RestAnalyticsProvider, url: String): AnalyticsProvider {
        return Proxy.newProxyInstance(
            this::class.java.classLoader,
            arrayOf(AnalyticsProvider::class.java, Closeable::class.java),
            MyAuthInvocationHandler(analyticsProvider, url)
        ) as AnalyticsProvider
    }


    private fun reportPosthogEvent(evenName: String, details: Map<String, String> = mapOf()) {
        findActiveProject()?.let { project ->
            ActivityMonitor.getInstance(project).registerCustomEvent(evenName, details)
        }
    }


    inner class MyAuthInvocationHandler(private val analyticsProvider: RestAnalyticsProvider, private val url: String) : InvocationHandler {

        override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
            return try {
                if (args == null) {
                    method.invoke(analyticsProvider)
                } else {
                    method.invoke(analyticsProvider, *args)
                }

            } catch (e: InvocationTargetException) {

                //check if this is an AuthenticationException, if not rethrow the exception
                @Suppress("UNUSED_VARIABLE")
                val authenticationException = ExceptionUtils.find(e, AuthenticationException::class.java)
                    ?: throw e

                Log.log(logger::info, "got AuthenticationException, calling loginOrRefresh")
                //if loginOrRefresh fails here there is no point in calling the method again.
                // it will probably fail again and will cause an endless loop.
                //just wait for the next invocation to try login or refresh again.
                if (loginOrRefresh(analyticsProvider, url, true)) {
                    Log.log(logger::info, "loginOrRefresh success after AuthenticationException")
                    if (args == null) {
                        method.invoke(analyticsProvider)
                    } else {
                        method.invoke(analyticsProvider, *args)
                    }
                } else {
                    Log.log(logger::info, "loginOrRefresh failed after AuthenticationException, rethrowing exception {}", e)
                    //must return a value ot throw the exception
                    throw e
                }
            }
        }
    }
}
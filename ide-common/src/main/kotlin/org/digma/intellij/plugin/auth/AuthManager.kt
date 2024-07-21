package org.digma.intellij.plugin.auth

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.Alarm
import org.digma.intellij.plugin.analytics.AnalyticsProvider
import org.digma.intellij.plugin.analytics.AuthenticationException
import org.digma.intellij.plugin.analytics.AuthenticationProvider
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.log.Log.API_LOGGER_NAME
import org.digma.intellij.plugin.settings.SettingsState
import java.io.Closeable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


@Service(Service.Level.APP)
class AuthManager : DisposableAdaptor {

    private val logger: Logger = Logger.getInstance(AuthManager::class.java)

    private val listeners: MutableList<AuthInfoChangeListener> = ArrayList()

    var apiUrl = SettingsState.getInstance().apiUrl
    private var myAnalyticsProvider: RestAnalyticsProvider = createMyAnalyticsProvider()

    private val fireChangeAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

    private val myLock = ReentrantLock()

    companion object {
        @JvmStatic
        fun getInstance(): AuthManager {
            return service<AuthManager>()
        }
    }


    private fun createMyAnalyticsProvider(): RestAnalyticsProvider {
        return RestAnalyticsProvider(listOf(DigmaAccessTokenAuthenticationProvider(SettingsTokenProvider())),
            { message: String? ->
                val apiLogger = Logger.getInstance(API_LOGGER_NAME)
                Log.log(apiLogger::debug, "API:AuthManager: {}", message)
            }, { apiUrl })
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

    @Suppress("MemberVisibilityCanBePrivate")
    fun removeAuthInfoChangeListener(listener: AuthInfoChangeListener) {
        listeners.remove(listener)
    }

    //withAuth should not be called concurrently by multiple threads. it is called only from AnalyticsService.createClient.
    //AnalyticsService.createClient is called on startup.
    //the analyticsProvider here is the one to wrap with a proxy, it's a per project analyticsProvider.
    //AuthManager uses its own local analyticsProvider for the login and refresh.
    fun withAuth(project: Project, analyticsProvider: RestAnalyticsProvider): AnalyticsProvider {

        return myLock.withLock {

            Log.log(logger::info, "wrapping analyticsProvider with auth for url {}", analyticsProvider.apiUrl)

            val loginHandler = LoginHandler.createLoginHandler(project, myAnalyticsProvider)

            if (!loginHandler.loginOrRefresh()) {
                Log.log(logger::warn, "loginOrRefresh failed for url {}", this.myAnalyticsProvider.apiUrl)
            }

            //always return a proxy. even if login failed. the proxy will try to loginOrRefresh on AuthenticationException
            val proxy = proxy(analyticsProvider)

            Log.log(
                logger::info, "analyticsProvider wrapped with auth proxy, current account {},analytics url {}",
                DigmaDefaultAccountHolder.getInstance().account,
                analyticsProvider.apiUrl
            )

            fireChange()

            proxy
        }
    }


    //this method is used to force login after changing the url.
    //and when user clicks refresh on no connection screen, in that case it will login or refresh
    // or do nothing if login already done. and will resume connection if connection is back.
    fun loginOrRefresh(): Boolean {

        try {

            myLock.lockInterruptibly()

            Log.log(logger::info, "loginOrRefresh called, analytics url {}", myAnalyticsProvider.apiUrl)

            val loginHandler = LoginHandler.createLoginHandler(myAnalyticsProvider)
            val result = loginHandler.loginOrRefresh()
            if (!result) {
                Log.log(logger::warn, "loginOrRefresh failed for url {}", this.myAnalyticsProvider.apiUrl)
            }

            fireChange()

            return result

        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "error in loginOrRefresh {}", e)
            ErrorReporter.getInstance().reportError("AuthManager.loginOrRefresh", e)
            return false
        } finally {
            if (myLock.isHeldByCurrentThread) {
                myLock.unlock()
            }
        }
    }


    fun login(user: String, password: String): LoginResult {

        try {

            myLock.lockInterruptibly()

            Log.log(logger::info, "login called, analytics url {}", myAnalyticsProvider.apiUrl)

            val loginHandler = LoginHandler.createLoginHandler(myAnalyticsProvider)
            val loginResult = loginHandler.login(user, password)

            Log.log(logger::info, "login result {}", loginResult)

            fireChange()

            return loginResult

        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "error in login {}", e)
            ErrorReporter.getInstance().reportError("AuthManager.login", e)
            return LoginResult(false, null, null)
        } finally {
            if (myLock.isHeldByCurrentThread) {
                myLock.unlock()
            }
        }
    }


    fun logout() {

        try {

            myLock.lockInterruptibly()

            Log.log(logger::info, "logout called, analytics url {}", myAnalyticsProvider.apiUrl)

            val loginHandler = LoginHandler.createLoginHandler(myAnalyticsProvider)
            loginHandler.logout()

            fireChange()

        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "error in login {}", e)
            ErrorReporter.getInstance().reportError("AuthManager.logout", e)
        } finally {
            if (myLock.isHeldByCurrentThread) {
                myLock.unlock()
            }
        }
    }


    private fun onAuthenticationException() {

        try {

            myLock.lockInterruptibly()

            Log.log(logger::trace, "onAuthenticationException called, analytics url {}", myAnalyticsProvider.apiUrl)

            val loginHandler = LoginHandler.createLoginHandler(myAnalyticsProvider)
            loginHandler.loginOrRefresh(true)

            fireChange()

        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "error in onAuthenticationException {}", e)
            ErrorReporter.getInstance().reportError("AuthManager.onAuthenticationException", e)
        } finally {
            if (myLock.isHeldByCurrentThread) {
                myLock.unlock()
            }
        }
    }


    private fun fireChange() {

        //don't fire immediately, wait a second.
        //maybe there was a logout and a login is going to happen just after that, so It's useless to fire twice,
        // it may cause the ui to show login screen for a second and then hide it.
        //probably waiting a second is ok in most cases.
        fireChangeAlarm.cancelAllRequests()
        fireChangeAlarm.addRequest({
            Log.log(
                logger::trace, "firing authInfoChanged, default account {}, analytics url {}",
                DigmaDefaultAccountHolder.getInstance().account,
                myAnalyticsProvider.apiUrl
            )

            val authInfo = AuthInfo(DigmaDefaultAccountHolder.getInstance().account?.userId)
            listeners.forEach {
                try {
                    Log.log(
                        logger::trace, "firing authInfoChanged to listener {}, default account {}, analytics url {}",
                        it,
                        DigmaDefaultAccountHolder.getInstance().account,
                        myAnalyticsProvider.apiUrl
                    )
                    it.authInfoChanged(authInfo)
                } catch (e: Throwable) {
                    ErrorReporter.getInstance().reportError("AuthManager.fireChange", e)
                }
            }
        }, 1000)
    }


    private fun proxy(analyticsProvider: RestAnalyticsProvider): AnalyticsProvider {
        return Proxy.newProxyInstance(
            this::class.java.classLoader,
            arrayOf(AnalyticsProvider::class.java, Closeable::class.java),
            MyAuthInvocationHandler(analyticsProvider)
        ) as AnalyticsProvider
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

                val authenticationException = ExceptionUtils.findCause(AuthenticationException::class.java, e)
                //log debug on AuthenticationException because it happens a lot, every time the token expires
                if (authenticationException == null) {
                    Log.warnWithException(logger, e, "Exception in auth proxy {}", ExceptionUtils.getNonEmptyMessage(e))
                } else {
                    Log.debugWithException(logger, e, "AuthenticationException in auth proxy {}", ExceptionUtils.getNonEmptyMessage(e))
                }

                //check if this is an AuthenticationException, if not rethrow the exception
                if (authenticationException == null) {
                    throw e
                }

                Log.log(logger::trace, "got AuthenticationException {}, calling onAuthenticationException", ExceptionUtils.getNonEmptyMessage(e))
                //call onAuthenticationException to refresh or login
                onAuthenticationException()

                //retry the method. if token was refreshed it should succeed , otherwise it will throw
                //AuthenticationException again that will be caught by plugin code
                if (args == null) {
                    method.invoke(analyticsProvider)
                } else {
                    method.invoke(analyticsProvider, *args)
                }
            }
        }
    }
}
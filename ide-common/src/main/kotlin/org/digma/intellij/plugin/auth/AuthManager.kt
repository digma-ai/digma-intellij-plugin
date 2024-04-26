package org.digma.intellij.plugin.auth

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.util.Alarm
import org.digma.intellij.plugin.analytics.AnalyticsProvider
import org.digma.intellij.plugin.analytics.AuthenticationException
import org.digma.intellij.plugin.analytics.AuthenticationProvider
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import java.io.Closeable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicBoolean


@Service(Service.Level.APP)
class AuthManager : Disposable {

    private val logger: Logger = Logger.getInstance(AuthManager::class.java)

    private val listeners: MutableList<AuthInfoChangeListener> = ArrayList()
    private var analyticsProvider: RestAnalyticsProvider? = null

    private val isPaused: AtomicBoolean = AtomicBoolean(true)

    private val fireChangeAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

    companion object {
        @JvmStatic
        fun getInstance(): AuthManager {
            return service<AuthManager>()
        }
    }

    override fun dispose() {
        //nothing to do, used as parent disposable
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

    //withAuth should not be called concurrently by multiple threads. it is called only from AnalyticsService.replaceClient.
    //AnalyticsService.replaceClient is called on startup, and when an api url in settings changed.
    //AnalyticsService.replaceClient and is synchronized and thus withAuth should not be called concurrently.
    //the analyticsProvider here will usually be a non proxied RestAnalyticsProvider
    //method is also synchronized to prevent concurrent execution
    @Synchronized
    fun withAuth(analyticsProvider: RestAnalyticsProvider): AnalyticsProvider {

        Log.log(logger::info, "wrapping analyticsProvider with auth for url {}", analyticsProvider.apiUrl)

        //keep the RestAnalyticsProvider every time withAuth is called, so it will always be the correct and latest one
        Log.log(logger::info, "setting new analyticsProvider url {}", analyticsProvider.apiUrl)
        this.analyticsProvider = analyticsProvider

        val loginHandler = LoginHandler.createLoginHandler(analyticsProvider)


        if (!loginHandler.loginOrRefresh()) {
            Log.log(logger::warn, "loginOrRefresh failed for url {}", this.analyticsProvider?.apiUrl)
        }

        //always return a proxy. even if login failed. the proxy will try to loginOrRefresh on AuthenticationException
        val proxy = proxy(analyticsProvider)

        Log.log(
            logger::info, "analyticsProvider wrapped with auth proxy, current account {},analytics url {}",
            DigmaDefaultAccountHolder.getInstance().account,
            analyticsProvider.apiUrl
        )

        Log.log(logger::info, "resuming current proxy, analytics url {}", this.analyticsProvider?.apiUrl)
        isPaused.set(false)

        fireChange()

        return proxy
    }


    @Synchronized
    fun login(user: String, password: String): LoginResult {

        Log.log(logger::info, "login called, analytics url {}", analyticsProvider?.apiUrl)

        val loginHandler = LoginHandler.createLoginHandler(analyticsProvider)
        val loginResult = loginHandler.login(user, password)

        Log.log(logger::info, "login result {}", loginResult)

        fireChange()
        return loginResult
    }


    @Synchronized
    fun logout() {

        Log.log(logger::info, "logout called, analytics url {}", analyticsProvider?.apiUrl)

        val loginHandler = LoginHandler.createLoginHandler(analyticsProvider)
        loginHandler.logout()

        fireChange()
    }


    @Synchronized
    private fun onAuthenticationException() {

        Log.log(logger::trace, "onAuthenticationException called, analytics url {}", analyticsProvider?.apiUrl)

        val loginHandler = LoginHandler.createLoginHandler(analyticsProvider)
        loginHandler.loginOrRefresh(true)

        fireChange()
    }


    private fun fireChange() {

        if (isPaused.get()) {
            fireChangeAlarm.cancelAllRequests()
            return
        }

        //don't fire immediately, wait a second.
        //maybe there was a logout and a login is going to happen just after that, so It's useless to fire twice,
        // it may cause the ui to show login screen for a second and then hide it.
        //probably waiting a second is ok in most cases.
        fireChangeAlarm.cancelAllRequests()
        fireChangeAlarm.addRequest({
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
                    ErrorReporter.getInstance().reportError("AuthManager.fireChange", e)
                }
            }
        }, 1000)
    }


    //pause the AuthManager before replacing the analytics provider.
    //can be resumed only from this class after a new client is set
    fun pauseBeforeClientChange() {
        Log.log(logger::info, "pausing current proxy, analytics url {}", analyticsProvider?.apiUrl)
        analyticsProvider = null
        isPaused.set(true)
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

                Log.debugWithException(logger, e, "Exception in auth proxy {}", ExceptionUtils.getNonEmptyMessage(e))
                if (isPaused.get()) {
                    Log.log(logger::trace, "got Exception in auth proxy but proxy is paused, rethrowing")
                    throw e
                }

                //check if this is an AuthenticationException, if not rethrow the exception
                @Suppress("UNUSED_VARIABLE")
                val authenticationException = ExceptionUtils.find(e, AuthenticationException::class.java)
                    ?: throw e

                Log.log(logger::trace, "got AuthenticationException, calling onAuthenticationException")

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
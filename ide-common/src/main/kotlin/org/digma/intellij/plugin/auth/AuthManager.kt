package org.digma.intellij.plugin.auth

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.Alarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.analytics.AnalyticsProvider
import org.digma.intellij.plugin.analytics.AnalyticsUrlProvider
import org.digma.intellij.plugin.analytics.AuthenticationException
import org.digma.intellij.plugin.analytics.AuthenticationProvider
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
import org.digma.intellij.plugin.auth.account.LoginHandler
import org.digma.intellij.plugin.auth.account.LoginResult
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.log.Log.API_LOGGER_NAME
import java.io.Closeable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.Semaphore


@Service(Service.Level.APP)
class AuthManager(private val cs: CoroutineScope) : Disposable {

    private val logger: Logger = Logger.getInstance(AuthManager::class.java)

    private val listeners: MutableList<AuthInfoChangeListener> = ArrayList()

    private var myAnalyticsProvider: RestAnalyticsProvider = createMyAnalyticsProvider()

    private val fireChangeAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

    private var loginOrRefreshJob: Job? = null

    private val loginLogoutSemaphore = Semaphore(1, true)

    companion object {
        @JvmStatic
        fun getInstance(): AuthManager {
            return service<AuthManager>()
        }
    }

    override fun dispose() {
        myAnalyticsProvider.close()
    }

    private fun createMyAnalyticsProvider(): RestAnalyticsProvider {
        //AuthManager sends higher order to receive url changed events, so it will replace
        // api client before the AnalyticsService
        return RestAnalyticsProvider(
            listOf(DigmaAccessTokenAuthenticationProvider(SettingsTokenProvider())),
            { message: String? ->
                val apiLogger = Logger.getInstance(API_LOGGER_NAME)
                Log.log(apiLogger::debug, "API:AuthManager: {}", message)
            }, AnalyticsUrlProvider.getInstance(), 1
        )
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

    /**
     * withAuth should not be called concurrently by multiple threads. it is called only from AnalyticsService.createClient,
     * which is actually the AnalyticsService constructor. the platform makes sure to create singleton service, the AnalyticsService
     * will not be called concurrently for the same project.
     * this method will be called concurrently if multiple projects are opened at the same time.
     * the analyticsProvider here is the one to wrap with a proxy, it's a per project analyticsProvider.
     * AuthManager uses its own local analyticsProvider for the login and refresh.
     */
    fun withAuth(project: Project, analyticsProvider: RestAnalyticsProvider): AnalyticsProvider {

        Log.log(logger::info, "wrapping analyticsProvider with auth for url {}", analyticsProvider.apiUrl)

        //try loginOrRefresh on startup
        loginOrRefresh(project)

        //always return a proxy. even if login failed. the proxy will try to loginOrRefresh on AuthenticationException
        val proxy = proxy(analyticsProvider)

        Log.log(
            logger::info, "analyticsProvider wrapped with auth proxy, current account {},analytics url {}",
            DigmaDefaultAccountHolder.getInstance().account,
            analyticsProvider.apiUrl
        )

        fireChange()

        return proxy
    }


    /**
     * this method is used to do best effort for login or refresh.
     * when connection to the server is ok it will succeed either new login or refresh if necessary.
     * it is non-blocking but does not allow more than one job at a time, if called concurrently and
     * myAuthJob is active the method will just return doing nothing.
     * it is mostly called when there is an AuthenticationException when the token is expired. if called by
     * multiple threads only one will start a job, other threads will return and probably will have to deal with
     * AuthenticationException at least until the refresh is complete.
     * it is possible to let multiple threads start a loginOrRefresh, it will work and will probably do no harm,
     * worst case a login or refresh will occur multiple times.
     * it is also possible to block the method until refresh completes and only then release the threads, either
     * by only one thread or multiple threads, but the current approaches is to launch an asynchronous, let threads deal
     * with AuthenticationException until refresh completes. in a good running environment this will be very fast so only
     * a few threads will deal with AuthenticationException.
     * loginHandler.loginOrRefresh is smart enough to decide if a new login is required, or refresh or nothing.
     */
    fun loginOrRefresh(project: Project? = null) {

        Log.log(logger::info, "loginOrRefresh called, analytics url {}", myAnalyticsProvider.apiUrl)

        if (loginOrRefreshJob?.isActive == true) {
            Log.log(logger::info, "auth job is active, analytics url {}", myAnalyticsProvider.apiUrl)
            return
        }

        loginOrRefreshJob?.cancel()
        loginOrRefreshJob = cs.launch {
            try {
                Log.log(logger::info, "starting loginOrRefresh job, analytics url {}", myAnalyticsProvider.apiUrl)
                val loginHandler = LoginHandler.createLoginHandler(project, myAnalyticsProvider)
                loginHandler.loginOrRefresh()
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "error in loginOrRefresh {}", e)
                ErrorReporter.getInstance().reportError("AuthManager.loginOrRefresh", e)
            } finally {
                fireChange()
            }
        }
    }


    /**
     * blocking login. should not be called by concurrent threads, this method should be called from UI
     * when a user clicks the login button.
     * every call will do a new login, logging out if necessary.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun loginSynchronously(user: String, password: String): LoginResult {

        //prevent multiple thread doing login, shouldn't happen as this method is called by UI when user clicks
        // a login button
        loginLogoutSemaphore.acquire()

        val waitingSemaphore = Semaphore(0, true)
        val deferred = cs.async {
            try {
                val loginHandler = LoginHandler.createLoginHandler(myAnalyticsProvider)
                loginHandler.login(user, password)
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "error in login {}", e)
                ErrorReporter.getInstance().reportError("AuthManager.login", e)
                LoginResult(false, null, null)
            } finally {
                waitingSemaphore.release()
                loginLogoutSemaphore.release()
            }
        }

        return try {
            waitingSemaphore.acquire()
            deferred.getCompleted()
        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "error in login {}", e)
            ErrorReporter.getInstance().reportError("AuthManager.login", e)
            return LoginResult(false, null, null)
        } finally {
            fireChange()
        }
    }


    /**
     * non blocking logout
     */
    fun logout() {
        cs.launch {
            try {
                val loginHandler = LoginHandler.createLoginHandler(myAnalyticsProvider)
                loginHandler.logout()
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "error in logout {}", e)
                ErrorReporter.getInstance().reportError("AuthManager.logout", e)
            } finally {
                fireChange()
            }
        }
    }


    /**
     * blocking logout. should not be called by concurrent threads, this method should be called as
     * result of user clicking logout.
     * every call will do log out if necessary.
     */
    fun logoutSynchronously() {

        //prevent multiple thread doing logout or login at the same time, shouldn't happen as this method is called by UI when user clicks
        // a logout button
        loginLogoutSemaphore.acquire()

        val waitingSemaphore = Semaphore(0, true)
        val job = cs.launch {
            try {
                val loginHandler = LoginHandler.createLoginHandler(myAnalyticsProvider)
                loginHandler.logout()
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "error in logoutSynchronously {}", e)
                ErrorReporter.getInstance().reportError("AuthManager.logoutSynchronously", e)
            } finally {
                waitingSemaphore.release()
                loginLogoutSemaphore.release()
            }
        }

        return try {
            waitingSemaphore.acquire()
        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "error in logoutSynchronously {}", e)
            ErrorReporter.getInstance().reportError("AuthManager.logoutSynchronously", e)
        } finally {
            job.cancel()
            fireChange()
        }
    }


    private fun onAuthenticationException() {
        loginOrRefresh()
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
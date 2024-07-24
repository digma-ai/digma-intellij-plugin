package org.digma.intellij.plugin.auth

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.Alarm
import kotlinx.coroutines.CoroutineName
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
import org.digma.intellij.plugin.auth.account.CredentialsHolder
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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Semaphore
import kotlin.time.Duration.Companion.seconds


@Service(Service.Level.APP)
class AuthManager(private val cs: CoroutineScope) : Disposable {

    private val logger: Logger = Logger.getInstance(AuthManager::class.java)

    //using CopyOnWriteArrayList to avoid ConcurrentModificationException.
    // a new opened project may try to add a listener while fireChange is iterating
    private val listeners = CopyOnWriteArrayList<AuthInfoChangeListener>()

    private var myAnalyticsProvider: RestAnalyticsProvider = createMyAnalyticsProvider()

    private val fireChangeAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

    private var loginOrRefreshAsyncJob: Job? = null

    private val loginLogoutSemaphore = Semaphore(1, true)

    //refreshTokenStrategy should be used only from onAuthenticationException
    private val refreshTokenStrategy = AuthManagerLockingRefreshStrategy(cs)
//    private val refreshTokenStrategy = AuthManagerNonLockingRefreshStrategy(cs)

    companion object {
        @JvmStatic
        fun getInstance(): AuthManager {
            return service<AuthManager>()
        }
    }

    override fun dispose() {
        myAnalyticsProvider.close()
        loginOrRefreshAsyncJob?.cancel()
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

        //try loginOrRefresh on startup, use loginOrRefreshAsync here because this code is called from
        // AnalyticsService constructor, and we don't want to block it, it's an attempt to complete the loginOrRefresh
        // before any api call is made. if it didn't succeed than the first api call will also do loginOrRefresh.
        //it may happen that eventually loginOrRefresh will be invoked more than once, it's ok, the server can deal with that.
        loginOrRefreshAsync(project)

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
     * this method is used for best effort to do login or refresh early on startup.
     * when connection to the server is ok it will succeed either new login or refresh if necessary.
     * it is non-blocking but does not allow more than one job at a time, if called concurrently and
     * myAuthJob is active the method will just return doing nothing.
     * it may be called concurrently if multiple projects open at the same time, worst case a login or refresh will
     * happen more than once, our server can deal with it.
     */
    fun loginOrRefreshAsync(project: Project? = null) {

        Log.log(logger::trace, "loginOrRefreshAsync called, analytics url {}", myAnalyticsProvider.apiUrl)

        if (loginOrRefreshAsyncJob?.isActive == true) {
            Log.log(logger::trace, "loginOrRefreshAsync job is active,aborting. analytics url {}", myAnalyticsProvider.apiUrl)
            return
        }

        loginOrRefreshAsyncJob?.cancel()
        Log.log(logger::trace, "launching loginOrRefreshAsync job, analytics url {}", myAnalyticsProvider.apiUrl)
        loginOrRefreshAsyncJob = cs.launch(CoroutineName("loginOrRefreshAsync")) {
            try {
                Log.log(logger::trace, "starting loginOrRefreshAsync job, analytics url {}", myAnalyticsProvider.apiUrl)
                val loginHandler = LoginHandler.createLoginHandler(project, myAnalyticsProvider)
                loginHandler.loginOrRefresh()
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "error in loginOrRefreshAsync {}", e)
                ErrorReporter.getInstance().reportError("AuthManager.loginOrRefresh", e)
            } finally {
                Log.log(logger::trace, "loginOrRefreshAsync job completed, analytics url {}", myAnalyticsProvider.apiUrl)
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
        try {

            Log.log(logger::trace, "acquire lock in loginSynchronously, analytics url {}", myAnalyticsProvider.apiUrl)
            loginLogoutSemaphore.acquire()

            val waitingSemaphore = Semaphore(0, true)
            Log.log(logger::trace, "launching loginSynchronously job, analytics url {}", myAnalyticsProvider.apiUrl)
            val deferred = cs.async(CoroutineName("loginSynchronously")) {
                try {
                    Log.log(logger::trace, "starting job loginSynchronously, analytics url {}", myAnalyticsProvider.apiUrl)
                    val loginHandler = LoginHandler.createLoginHandler(myAnalyticsProvider)
                    loginHandler.login(user, password)
                } catch (e: Throwable) {
                    Log.warnWithException(logger, e, "error in loginSynchronously {}", e)
                    ErrorReporter.getInstance().reportError("AuthManager.login", e)
                    LoginResult(false, null, e.message)
                } finally {
                    Log.log(logger::trace, "job loginSynchronously completed, releasing wait lock, analytics url {}", myAnalyticsProvider.apiUrl)
                    waitingSemaphore.release()
                }
            }

            try {
                Log.log(logger::trace, "waiting for loginSynchronously job to complete, analytics url {}", myAnalyticsProvider.apiUrl)
                waitingSemaphore.acquire()
                Log.log(
                    logger::trace,
                    "done waiting for loginSynchronously job, completed successfully, analytics url {}",
                    myAnalyticsProvider.apiUrl
                )
                return deferred.getCompleted()
            } catch (e: Throwable) {
                deferred.cancel()
                Log.warnWithException(logger, e, "error in loginSynchronously {}", e)
                ErrorReporter.getInstance().reportError("AuthManager.login", e)
                return LoginResult(false, null, e.message)
            }

        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "error in loginSynchronously {}", e)
            ErrorReporter.getInstance().reportError("AuthManager.login", e)
            return LoginResult(false, null, e.message)
        } finally {
            Log.log(logger::trace, "releasing lock in loginSynchronously")
            loginLogoutSemaphore.release()
            fireChange()
        }
    }


    /**
     * non blocking logout
     */
    fun logoutAsync() {
        Log.log(logger::trace, "launching logoutAsync job, analytics url {}", myAnalyticsProvider.apiUrl)
        cs.launch(CoroutineName("logoutAsync")) {
            try {
                Log.log(logger::trace, "starting job logoutAsync, analytics url {}", myAnalyticsProvider.apiUrl)
                val loginHandler = LoginHandler.createLoginHandler(myAnalyticsProvider)
                loginHandler.logout()
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "error in logoutAsync {}", e)
                ErrorReporter.getInstance().reportError("AuthManager.logout", e)
            } finally {
                Log.log(logger::trace, "job logoutAsync completed, analytics url {}", myAnalyticsProvider.apiUrl)
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
        try {
            Log.log(logger::trace, "acquire lock in logoutSynchronously, analytics url {}", myAnalyticsProvider.apiUrl)
            loginLogoutSemaphore.acquire()

            val waitingSemaphore = Semaphore(0, true)
            Log.log(logger::trace, "launching logoutSynchronously job, analytics url {}", myAnalyticsProvider.apiUrl)
            val job = cs.launch(CoroutineName("logoutSynchronously")) {
                try {
                    Log.log(logger::trace, "starting job logoutSynchronously, analytics url {}", myAnalyticsProvider.apiUrl)
                    val loginHandler = LoginHandler.createLoginHandler(myAnalyticsProvider)
                    loginHandler.logout()
                } catch (e: Throwable) {
                    Log.warnWithException(logger, e, "error in logoutSynchronously {}", e)
                    ErrorReporter.getInstance().reportError("AuthManager.logout", e)
                } finally {
                    Log.log(logger::trace, "job logoutSynchronously completed,releasing wait lock, analytics url {}", myAnalyticsProvider.apiUrl)
                    waitingSemaphore.release()
                }
            }

            try {
                Log.log(logger::trace, "waiting for logoutSynchronously job to complete, analytics url {}", myAnalyticsProvider.apiUrl)
                waitingSemaphore.acquire()
                Log.log(logger::trace, "done waiting for logoutSynchronously, completed successfully, analytics url {}", myAnalyticsProvider.apiUrl)
            } catch (e: Throwable) {
                job.cancel()
                Log.warnWithException(logger, e, "error in logoutSynchronously {}", e)
                ErrorReporter.getInstance().reportError("AuthManager.logout", e)
            }

        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "error in logoutSynchronously {}", e)
            ErrorReporter.getInstance().reportError("AuthManager.logout", e)
        } finally {
            Log.log(logger::trace, "releasing lock in logoutSynchronously")
            loginLogoutSemaphore.release()
            fireChange()
        }
    }


    private fun onAuthenticationException() {
        Log.log(logger::trace, "onAuthenticationException called, analytics url {}", myAnalyticsProvider.apiUrl)
        refreshTokenStrategy.loginOrRefresh(true)
        Log.log(logger::trace, "onAuthenticationException completed, analytics url {}", myAnalyticsProvider.apiUrl)
    }


    //todo: don't fire too much, if there is no login,when user got the login page but didn't yet login, many threads will
    // fail on 401 and we fire the event every time, listener will act on every event, for example BackendInfoHolder,
    // its probably not necessary to fire when there is no real change.
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


    private interface RefreshStrategy {
        fun loginOrRefresh(onAuthenticationError: Boolean = false)
    }


    private inner class AuthManagerLockingRefreshStrategy(private val cs: CoroutineScope) : RefreshStrategy {

        private val logger: Logger = Logger.getInstance(AuthManagerLockingRefreshStrategy::class.java)

        private val loginOrRefreshSemaphore = Semaphore(1, true)

        override fun loginOrRefresh(onAuthenticationError: Boolean) {

            if (tokenValid()) {
                Log.log(logger::trace, "loginOrRefresh called, credentials is valid, aborting")
                return
            }


            try {
                Log.log(logger::trace, "acquiring lock in loginOrRefresh")
                loginOrRefreshSemaphore.acquire()
                if (tokenValid()) {
                    Log.log(logger::trace, "loginOrRefresh called, credentials is valid, releasing lock and aborting")
                    loginOrRefreshSemaphore.release()
                    return
                }


                val waitingSemaphore = Semaphore(0, true)
                Log.log(logger::trace, "launching loginOrRefresh job, analytics url {}", myAnalyticsProvider.apiUrl)
                val job = cs.launch(CoroutineName("lockingLoginOrRefresh")) {
                    try {
                        Log.log(logger::trace, "starting lockingLoginOrRefresh job, analytics url {}", myAnalyticsProvider.apiUrl)
                        val loginHandler = LoginHandler.createLoginHandler(null, myAnalyticsProvider)
                        loginHandler.loginOrRefresh(onAuthenticationError)
                    } catch (e: Throwable) {
                        Log.warnWithException(logger, e, "error in lockingLoginOrRefresh job {}", e)
                        ErrorReporter.getInstance().reportError("AuthManager.loginOrRefresh", e)
                    } finally {
                        Log.log(
                            logger::trace,
                            "lockingLoginOrRefresh job completed, releasing wait lock, analytics url {}",
                            myAnalyticsProvider.apiUrl
                        )
                        waitingSemaphore.release()
                    }
                }

                try {
                    Log.log(logger::trace, "waiting for loginOrRefresh job to complete, analytics url {}", myAnalyticsProvider.apiUrl)
                    waitingSemaphore.acquire()
                    Log.log(logger::trace, "done waiting for loginOrRefresh, completed successfully, analytics url {}", myAnalyticsProvider.apiUrl)
                } catch (e: Throwable) {
                    job.cancel()
                    Log.warnWithException(logger, e, "error while waiting for loginOrRefresh {}", e)
                    ErrorReporter.getInstance().reportError("AuthManager.loginOrRefresh", e)
                }

            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "error in loginOrRefresh {}", e)
                ErrorReporter.getInstance().reportError("AuthManager.loginOrRefresh", e)
            } finally {
                Log.log(logger::trace, "releasing lock in loginOrRefresh")
                loginOrRefreshSemaphore.release()
                fireChange()
            }
        }
    }


    //this strategy may do multiple refresh one after the other
    private inner class AuthManagerNonLockingRefreshStrategy(private val cs: CoroutineScope) : RefreshStrategy {

        private val logger: Logger = Logger.getInstance(AuthManagerNonLockingRefreshStrategy::class.java)

        override fun loginOrRefresh(onAuthenticationError: Boolean) {

            if (tokenValid()) {
                Log.log(logger::trace, "loginOrRefresh called, credentials is valid, aborting")
                return
            }

            val waitingSemaphore = Semaphore(0, true)
            Log.log(logger::trace, "launching loginOrRefresh job, analytics url {}", myAnalyticsProvider.apiUrl)
            val job = cs.launch(CoroutineName("nonLockingLoginOrRefresh")) {
                try {
                    Log.log(logger::trace, "starting nonLockingLoginOrRefresh job, analytics url {}", myAnalyticsProvider.apiUrl)
                    val loginHandler = LoginHandler.createLoginHandler(myAnalyticsProvider)
                    loginHandler.loginOrRefresh(onAuthenticationError)
                } catch (e: Throwable) {
                    Log.warnWithException(logger, e, "error in nonLockingLoginOrRefresh job {}", e)
                    ErrorReporter.getInstance().reportError("AuthManager.loginOrRefresh", e)
                } finally {
                    Log.log(
                        logger::trace,
                        "nonLockingLoginOrRefresh job completed, releasing wait lock, analytics url {}",
                        myAnalyticsProvider.apiUrl
                    )
                    waitingSemaphore.release()
                }
            }

            try {
                Log.log(logger::trace, "waiting for loginOrRefresh job to complete, analytics url {}", myAnalyticsProvider.apiUrl)
                waitingSemaphore.acquire()
                Log.log(logger::trace, "done waiting for loginOrRefresh, completed successfully, analytics url {}", myAnalyticsProvider.apiUrl)
            } catch (e: Throwable) {
                job.cancel()
                Log.warnWithException(logger, e, "error while waiting for loginOrRefresh {}", e)
                ErrorReporter.getInstance().reportError("AuthManager.loginOrRefresh", e)
            } finally {
                fireChange()
            }
        }
    }


    private fun tokenValid(): Boolean {
        //luckily we have the CredentialsHolder that was born for other needs, we can use it to test
        // if refresh is necessary before locking.
        //in this context the token is valid if created in the past 30 seconds, we can than assume
        // that it was created lately probably by a preceding thread, and we don't need to refresh again.
        //if the token is not expired, but is older than 30 than we can't be sure It's still really valid, because
        // we are in the context of authentication exception, there is a reason we got the exception, it could be
        // this is a thread that waited for a preceding thread to do the refresh, i that case surely the token is very young,
        // if the token was refreshed 2 minutes ago then why did we get the exception?
        //these are assumptions that will probably work all the time.
        //we can not completely rely on the expiration time alone.
        return CredentialsHolder.digmaCredentials?.isAccessTokenValid() == true
                && CredentialsHolder.digmaCredentials?.isYoungerThen(30.seconds) == true
    }


}

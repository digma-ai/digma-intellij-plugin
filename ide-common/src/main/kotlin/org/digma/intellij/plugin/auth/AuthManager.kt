package org.digma.intellij.plugin.auth

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.Alarm
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.digma.intellij.plugin.analytics.AnalyticsProvider
import org.digma.intellij.plugin.analytics.AnalyticsUrlProvider
import org.digma.intellij.plugin.analytics.ApiErrorHandler
import org.digma.intellij.plugin.analytics.AuthenticationException
import org.digma.intellij.plugin.analytics.AuthenticationProvider
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.auth.account.CredentialsHolder
import org.digma.intellij.plugin.auth.account.DigmaAccountManager
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
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@Service(Service.Level.APP)
class AuthManager(private val cs: CoroutineScope) : Disposable {

    private val logger: Logger = Logger.getInstance(AuthManager::class.java)

    //using CopyOnWriteArrayList to avoid ConcurrentModificationException.
    // a new opened project may try to add a listener while fireChange is iterating
    private val listeners = CopyOnWriteArrayList<AuthInfoChangeListener>()

    private var myAnalyticsProvider: RestAnalyticsProvider = createMyAnalyticsProvider()

    private val fireChangeAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

    private var myLatestAuthInfo =
        AuthInfo(DigmaDefaultAccountHolder.getInstance().account?.userId, DigmaDefaultAccountHolder.getInstance().account?.id)

    private val loginLogoutSemaphore = Semaphore(1, true)

    //refreshTokenStrategy should be used only from onAuthenticationException
    private val refreshTokenStrategy = AuthManagerLockingRefreshStrategy(cs)
//    private val refreshTokenStrategy = AuthManagerNonLockingRefreshStrategy(cs)

    private var autoRefreshJob: Job? = null
    private var autoRefreshWaitingJob: Job? = null

    companion object {
        @JvmStatic
        fun getInstance(): AuthManager {
            return service<AuthManager>()
        }
    }


    init {
        autoRefreshJob = startAutoRefreshJob(cs)
    }

    override fun dispose() {
        myAnalyticsProvider.close()
        autoRefreshWaitingJob?.cancel()
        autoRefreshJob?.cancel()
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
     * constructor will not be called concurrently for the same project.
     * this method will be called concurrently if multiple projects are opened at the same time.
     * the analyticsProvider here is the one to wrap with a proxy, it's a per project analyticsProvider.
     * AuthManager uses its own local analyticsProvider for the login and refresh.
     */
    fun withAuth(project: Project, analyticsProvider: RestAnalyticsProvider): AnalyticsProvider {

        Log.log(logger::info, "wrapping analyticsProvider with auth for url {}", analyticsProvider.apiUrl)

        //try to update the CredentialsHolder as early as possible. if there is an account and credentials then after updating CredentialsHolder
        // api calls will already have authentication. this is necessary only on startup of the IDE when there are possibly
        // running tasks already, for example refresh environments starts very early.
        // after startup CredentialsHolder is always updated.
        updateCredentialsHolder()

        //try if there is connection to the api and mark connection lost/gained very early on startup of the project
        tryConnection(project)

        //always return a proxy. even if there is no connection. the proxy will try to loginOrRefresh on AuthenticationException
        val proxy = proxy(analyticsProvider)

        Log.log(
            logger::info, "analyticsProvider wrapped with auth proxy, current account {},analytics url {}",
            DigmaDefaultAccountHolder.getInstance().account,
            analyticsProvider.apiUrl
        )

        fireChange()

        return proxy
    }


    private fun tryConnection(project: Project) {

        Log.log(logger::trace, "launching tryConnection job")

        cs.launch(CoroutineName("tryConnection")) {
            try {
                Log.log(logger::trace, "${coroutineContext[CoroutineName]}: trying to call getAbout")
                //just call getAbout, if it succeeds then connection is ok.
                //if getAbout fails nothing will work because we need it to decide which authentication to do,local or centralized.
                myAnalyticsProvider.about
                Log.log(logger::trace, "${coroutineContext[CoroutineName]}: getAbout succeeded")

                //if there is connection then reset the connection status is necessary.
                //in extreme cases where many projects are opened on startup. it may happen that a project got
                // connection lost event, and if connection gained very quickly maybe some listeners where not active yet and did not
                // get the connection gained event. this call will fix it. if connection is ok this will be a very fast call and will do
                // no harm if there is nothing to do. it happens only on startup, called from withAuth which is called only once on project startup.
                ApiErrorHandler.getInstance().resetConnectionLostAndNotifyIfNecessary(project)

            } catch (e: Throwable) {

                Log.warnWithException(logger, e, "${coroutineContext[CoroutineName]}: getAbout failed with exception {}", e)
                //if we can't call getAbout we assume there is a connection issue, it may be a real connect issue,
                // or any other issue were we can't get analyticsProvider.about
                ApiErrorHandler.getInstance().handleAuthManagerCantConnectError(e, project)
            }
        }
    }


    private fun updateCredentialsHolder() {

        Log.log(logger::trace, "updateCredentialsHolder called")

        //if CredentialsHolder already has credentials return, this is the usual case.
        //if there is no account yet return, there is nothing to do
        if (CredentialsHolder.digmaCredentials != null || DigmaDefaultAccountHolder.getInstance().account == null) {
            Log.log(logger::trace, "not updating credentials because already updated")
            return
        }

        Log.log(logger::trace, "launching updateCredentialsHolder job")
        cs.launch(CoroutineName("updateCredentialsHolder")) {
            try {
                val account = DigmaDefaultAccountHolder.getInstance().account
                account?.let { acc ->
                    Log.log(logger::trace, "${coroutineContext[CoroutineName]}: found account {}", acc)
                    val credentials = DigmaAccountManager.getInstance().findCredentials(acc)
                    credentials?.let { creds ->
                        Log.log(
                            logger::trace,
                            "${coroutineContext[CoroutineName]}: found credentials, updating CredentialsHolder with account {}",
                            acc
                        )
                        CredentialsHolder.digmaCredentials = creds
                        fireChange()
                    }
                }

            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "${coroutineContext[CoroutineName]}: error in updateCredentialsHolder {}", e)
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

        Log.log(logger::trace, "loginSynchronously called")
        //prevent multiple thread doing login, shouldn't happen as this method is called by UI when user clicks
        // a login button
        try {

            Log.log(logger::trace, "acquire lock in loginSynchronously, analytics url {}", myAnalyticsProvider.apiUrl)
            loginLogoutSemaphore.acquire()
            Log.log(logger::trace, "lock acquired in loginSynchronously")

            val waitingSemaphore = Semaphore(0, true)
            Log.log(logger::trace, "launching loginSynchronously job, analytics url {}", myAnalyticsProvider.apiUrl)
            val deferred = cs.async(CoroutineName("loginSynchronously")) {
                try {
                    Log.log(logger::trace, "starting job loginSynchronously, analytics url {}", myAnalyticsProvider.apiUrl)
                    val loginHandler = LoginHandler.createLoginHandler(myAnalyticsProvider, true)
                    val loginResult = loginHandler.login(user, password, "loginSynchronously called")
                    //wake up the auto refresh to check the next auto refresh time
                    cancelAutoRefreshWaitingJob("from loginSynchronously")
                    loginResult
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
            Log.log(logger::trace, "lock released in loginSynchronously")
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
                loginHandler.logout("logoutAsync called")
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

        Log.log(logger::trace, "logoutSynchronously called")
        //prevent multiple thread doing logout or login at the same time, shouldn't happen as this method is called by UI when user clicks
        // a logout button
        try {
            Log.log(logger::trace, "acquire lock in logoutSynchronously, analytics url {}", myAnalyticsProvider.apiUrl)
            loginLogoutSemaphore.acquire()
            Log.log(logger::trace, "lock acquired in logoutSynchronously")

            val waitingSemaphore = Semaphore(0, true)
            Log.log(logger::trace, "launching logoutSynchronously job, analytics url {}", myAnalyticsProvider.apiUrl)
            val job = cs.launch(CoroutineName("logoutSynchronously")) {
                try {
                    Log.log(logger::trace, "starting job logoutSynchronously, analytics url {}", myAnalyticsProvider.apiUrl)
                    val loginHandler = LoginHandler.createLoginHandler(myAnalyticsProvider)
                    loginHandler.logout("logoutSynchronously called")
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
            Log.log(logger::trace, "lock released in logoutSynchronously")
            fireChange()
        }
    }


    private fun onAuthenticationException(methodName: String) {
        Log.log(logger::trace, "onAuthenticationException called, analytics url {}", myAnalyticsProvider.apiUrl)
        refreshTokenStrategy.loginOrRefresh(true, methodName)
        fireChange()
        Log.log(logger::trace, "onAuthenticationException completed, analytics url {}", myAnalyticsProvider.apiUrl)
    }


    private fun fireChange() {

        Log.log(logger::trace, "fireChange called")

        //don't fire immediately, wait a second.
        //maybe there was a logout and a login is going to happen just after that, so It's useless to fire twice,
        // it may cause the ui to show login screen for a second and then hide it.
        //probably waiting a second is ok in most cases.
        fireChangeAlarm.cancelAllRequests()
        fireChangeAlarm.addRequest({


            val authInfo = AuthInfo(DigmaDefaultAccountHolder.getInstance().account?.userId, DigmaDefaultAccountHolder.getInstance().account?.id)
            Log.log(logger::trace, "in fireChange, current auth info: {}, myLatestAuthInfo: {}", authInfo, myLatestAuthInfo)
            //fire the event only when there is a real change
            if (authInfo == myLatestAuthInfo) {
                Log.log(logger::trace, "in fireChange, auth info not changed , not firing event")
            } else {

                myLatestAuthInfo = authInfo

                Log.log(logger::trace, "in fireChange, auth info changed , firing event {}", myLatestAuthInfo)

                withAuthManagerDebug {
                    reportAuthPosthogEvent(
                        "fire auth info changed",
                        this.javaClass.simpleName,
                        mapOf("user.id" to myLatestAuthInfo.userId.toString(), "listeners" to listeners.size)
                    )
                }

                Log.log(
                    logger::trace, "firing authInfoChanged, default account {}, analytics url {}",
                    DigmaDefaultAccountHolder.getInstance().account,
                    myAnalyticsProvider.apiUrl
                )

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
                        Log.warnWithException(logger, e, "error from AuthInfoChangeListener {}", it)
                        ErrorReporter.getInstance().reportError("AuthManager.fireChange", e)
                    }
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


    fun stopAutoRefresh(message: String) {
        try {
            Log.log(logger::trace, "stopping autoRefreshJob {}", message)
            autoRefreshWaitingJob?.cancel(CancellationException(message, null))
            autoRefreshJob?.cancel(CancellationException(message, null))
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("AuthManager.stopAutoRefresh", e)
        }
    }

    fun startAutoRefresh() {
        try {
            Log.log(logger::trace, "starting autoRefreshJob")
            autoRefreshJob = startAutoRefreshJob(cs)
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("AuthManager.startAutoRefresh", e)
        }
    }

    private fun cancelAutoRefreshWaitingJob(message: String) {
        try {
            Log.log(logger::trace, "canceling autoRefreshJob.autoRefreshWaitingJob {}", message)
            autoRefreshWaitingJob?.cancel(CancellationException(message, null))
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("AuthManager.cancelAutoRefreshWaitingJob", e)
        }
    }


    //auto refresh one minute before expiration
    //Experimental
    private fun startAutoRefreshJob(cs: CoroutineScope): Job {

        if (autoRefreshJob?.isActive == true) {
            autoRefreshJob?.cancel()
        }

        Log.log(logger::trace, "launching autoRefreshJob")
        return cs.launch(CoroutineName("autoRefreshJob")) {

            Log.log(logger::trace, "${coroutineContext[CoroutineName]} auto refresh job started")
            var delay: Duration
            while (isActive) {

                try {

                    if (ApiErrorHandler.getInstance().isNoConnectionMode()) {
                        Log.log(logger::trace, "${coroutineContext[CoroutineName]} no connection, waiting 10 minutes")
                        delay = 10.minutes
                    } else {

                        Log.log(logger::trace, "${coroutineContext[CoroutineName]} looking for account")

                        val account = DigmaDefaultAccountHolder.getInstance().account
                        val credentials = account?.let {
                            try {
                                //exception here may happen if we change the credentials structure,which doesn't happen too much,
                                // user will be redirected to log in again
                                DigmaAccountManager.getInstance().findCredentials(it)
                            } catch (_: Throwable) {
                                null
                            }
                        }

                        if (account == null) {
                            Log.log(logger::trace, "${coroutineContext[CoroutineName]} account not found, waiting 10 minutes")
                            delay = 10.minutes
                        } else if (credentials == null) {
                            //if credentials not found it will probably be created soon
                            Log.log(logger::trace, "${coroutineContext[CoroutineName]} credentials for account not found, waiting 1 minute")
                            delay = 1.minutes
                        } else {
                            Log.log(logger::trace, "${coroutineContext[CoroutineName]} found account and credentials {}", account)

                            val expireIn =
                                max(
                                    0,
                                    (credentials.expirationTime - Clock.System.now().toEpochMilliseconds())
                                ).toDuration(DurationUnit.MILLISECONDS)

                            if (expireIn <= 1.minutes) {
                                Log.log(
                                    logger::trace,
                                    "${coroutineContext[CoroutineName]} credentials expires in {} , refreshing account {}",
                                    expireIn,
                                    account
                                )

                                val refreshSuccess = try {
                                    val loginHandler = LoginHandler.createLoginHandler(myAnalyticsProvider)
                                    loginHandler.refresh(account, credentials, "auto refresh job")
                                } catch (e: Throwable) {
                                    false
                                }

                                if (refreshSuccess) {
                                    fireChange()
                                    Log.log(logger::trace, "${coroutineContext[CoroutineName]} credentials for account refreshed {}", account)
                                    //immediately loop again and compute the next delay
                                    delay = ZERO
                                } else {
                                    Log.log(logger::trace, "${coroutineContext[CoroutineName]} refresh failed, waiting 5 minutes")
                                    delay = 5.minutes
                                }
                            } else {
                                delay =
                                    max(0L, (expireIn.inWholeMilliseconds - 1.minutes.inWholeMilliseconds)).toDuration(DurationUnit.MILLISECONDS)
                                Log.log(
                                    logger::trace,
                                    "${coroutineContext[CoroutineName]} credentials for account expires in {}, waiting {}",
                                    expireIn,
                                    delay
                                )
                            }
                        }
                    }

                    if (delay > ZERO) {
                        autoRefreshWaitingJob = launch {
                            Log.log(logger::trace, "${coroutineContext[CoroutineName]} in autoRefreshJob.autoRefreshWaitingJob waiting {}", delay)
                            delay(delay.inWholeMilliseconds)
                            Log.log(logger::trace, "${coroutineContext[CoroutineName]} in autoRefreshJob.autoRefreshWaitingJob done")
                        }

                        Log.log(logger::trace, "${coroutineContext[CoroutineName]} waiting for autoRefreshWaitingJob {}", delay)
                        val started = Clock.System.now()
                        autoRefreshWaitingJob?.join()
                        Log.log(
                            logger::trace,
                            "${coroutineContext[CoroutineName]} autoRefreshWaitingJob exited after {}",
                            (Clock.System.now() - started).absoluteValue
                        )
                    }

                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Log.warnWithException(logger, e, "${coroutineContext[CoroutineName]} error in autoRefreshJob")
                    ErrorReporter.getInstance().reportError("AuthManager.autoRefreshJob", e)
                    Log.log(logger::trace, "${coroutineContext[CoroutineName]} error from refresh, waiting 5 minutes")
                }
            }

            Log.log(logger::trace, "job autoRefreshJob exited (this should happen on IDE shutdown or when replacing api client)")
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

                Log.log(
                    logger::trace,
                    "got AuthenticationException {} for method {}, calling onAuthenticationException",
                    authenticationException.message,
                    method.name
                )
                //call onAuthenticationException to refresh or login
                onAuthenticationException(method.name)

                Log.log(logger::trace, "after AuthenticationException, retrying method {}", method.name)
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


    //RefreshStrategy is designed to be called only from onAuthenticationException
    private interface RefreshStrategy {
        fun loginOrRefresh(onAuthenticationError: Boolean = false, methodName: String)
    }


    private inner class AuthManagerLockingRefreshStrategy(private val cs: CoroutineScope) : RefreshStrategy {

        private val logger: Logger = Logger.getInstance(AuthManagerLockingRefreshStrategy::class.java)

        private val loginOrRefreshSemaphore = Semaphore(1, true)

        override fun loginOrRefresh(onAuthenticationError: Boolean, methodName: String) {

            Log.log(logger::trace, "lockingLoginOrRefresh called for {}", methodName)

            if (tokenValid()) {
                Log.log(logger::trace, "lockingLoginOrRefresh called, credentials is valid, aborting")
                return
            }


            try {
                Log.log(logger::trace, "trying to acquire lock in lockingLoginOrRefresh for {}", methodName)
                loginOrRefreshSemaphore.acquire()
                Log.log(logger::trace, "lock acquired in lockingLoginOrRefresh for {}", methodName)
                if (tokenValid()) {
                    Log.log(logger::trace, "lockingLoginOrRefresh called, credentials is valid, releasing lock and aborting for {}", methodName)
                    loginOrRefreshSemaphore.release()
                    Log.log(logger::trace, "lock released in lockingLoginOrRefresh for {}", methodName)
                    return
                }


                val errorHandler = CoroutineExceptionHandler { _, exception ->
                    ApiErrorHandler.getInstance().handleAuthManagerCantRefreshError(exception)
                }

                val waitingSemaphore = Semaphore(0, true)
                Log.log(logger::trace, "launching lockingLoginOrRefresh job, analytics url {}, method {}", myAnalyticsProvider.apiUrl, methodName)
                val job = cs.launch(CoroutineName("lockingLoginOrRefresh") + errorHandler) {
                    try {
                        Log.log(
                            logger::trace,
                            "starting lockingLoginOrRefresh job, analytics url {}, method {}",
                            myAnalyticsProvider.apiUrl,
                            methodName
                        )
                        val loginHandler = LoginHandler.createLoginHandler(myAnalyticsProvider, true)
                        loginHandler.loginOrRefresh(onAuthenticationError, "lockingLoginOrRefresh on authentication exception for $methodName")
                        //wake up the auto refresh to check the next auto refresh time
                        cancelAutoRefreshWaitingJob("from lockingLoginOrRefresh for $methodName")
                    } catch (e: Throwable) {
                        Log.warnWithException(logger, e, "error in lockingLoginOrRefresh job {}", e)
                        ErrorReporter.getInstance().reportError("AuthManager.loginOrRefresh", e)
                        throw e
                    } finally {
                        Log.log(
                            logger::trace,
                            "lockingLoginOrRefresh job completed, releasing wait lock, analytics url {},method {}",
                            myAnalyticsProvider.apiUrl, methodName
                        )
                        waitingSemaphore.release()
                    }
                }

                try {
                    Log.log(
                        logger::trace,
                        "waiting for lockingLoginOrRefresh job to complete, analytics url {},method {}",
                        myAnalyticsProvider.apiUrl,
                        methodName
                    )
                    waitingSemaphore.acquire()
                    Log.log(
                        logger::trace,
                        "done waiting for lockingLoginOrRefresh, completed successfully, analytics url {},method {}",
                        myAnalyticsProvider.apiUrl, methodName
                    )
                } catch (e: Throwable) {
                    job.cancel()
                    Log.warnWithException(logger, e, "error while waiting for lockingLoginOrRefresh {}", e)
                    ErrorReporter.getInstance().reportError("AuthManager.loginOrRefresh", e)
                }

            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "error in lockingLoginOrRefresh {}", e)
                ErrorReporter.getInstance().reportError("AuthManager.loginOrRefresh", e)
            } finally {
                Log.log(logger::trace, "releasing lock in lockingLoginOrRefresh for method {}", methodName)
                loginOrRefreshSemaphore.release()
                Log.log(logger::trace, "lock released in lockingLoginOrRefresh for method {}", methodName)
                fireChange()
            }
        }
    }


    //todo: delete , not used
    //this strategy may do multiple refresh one after the other
    //this class was an experiment, it has been tested to work, but we don't use it
    @Suppress("unused")
    private inner class AuthManagerNonLockingRefreshStrategy(private val cs: CoroutineScope) : RefreshStrategy {

        private val logger: Logger = Logger.getInstance(AuthManagerNonLockingRefreshStrategy::class.java)

        override fun loginOrRefresh(onAuthenticationError: Boolean, methodName: String) {

            if (tokenValid()) {
                Log.log(logger::trace, "nonLockingLoginOrRefresh called, credentials is valid, aborting")
                return
            }

            val waitingSemaphore = Semaphore(0, true)
            Log.log(logger::trace, "launching nonLockingLoginOrRefresh job, analytics url {}", myAnalyticsProvider.apiUrl)
            val job = cs.launch(CoroutineName("nonLockingLoginOrRefresh")) {
                try {
                    Log.log(logger::trace, "starting nonLockingLoginOrRefresh job, analytics url {}", myAnalyticsProvider.apiUrl)
                    val loginHandler = LoginHandler.createLoginHandler(myAnalyticsProvider, true)
                    loginHandler.loginOrRefresh(onAuthenticationError, "nonLockingLoginOrRefresh on authentication exception for $methodName")
                    //wake up the auto refresh to check the next auto refresh time
                    cancelAutoRefreshWaitingJob("from nonLockingLoginOrRefresh")
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
                Log.log(logger::trace, "waiting for nonLockingLoginOrRefresh job to complete, analytics url {}", myAnalyticsProvider.apiUrl)
                waitingSemaphore.acquire()
                Log.log(
                    logger::trace,
                    "done waiting for nonLockingLoginOrRefresh, completed successfully, analytics url {}",
                    myAnalyticsProvider.apiUrl
                )
            } catch (e: Throwable) {
                job.cancel()
                Log.warnWithException(logger, e, "error while waiting for nonLockingLoginOrRefresh {}", e)
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

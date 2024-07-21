package org.digma.intellij.plugin.analytics

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.util.Alarm
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.ExceptionUtils.findConnectException
import org.digma.intellij.plugin.common.ExceptionUtils.findFirstRealExceptionCause
import org.digma.intellij.plugin.common.ExceptionUtils.findSslException
import org.digma.intellij.plugin.common.ExceptionUtils.getNonEmptyMessage
import org.digma.intellij.plugin.common.ExceptionUtils.isEOFException
import org.digma.intellij.plugin.common.argsToString
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.notifications.NotificationUtil
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Objects
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer


@Service(Service.Level.APP)
class ApiErrorHandler : DisposableAdaptor {

    private val logger: Logger = Logger.getInstance(ApiErrorHandler::class.java)

    companion object {
        @JvmStatic
        fun getInstance(): ApiErrorHandler {
            return service<ApiErrorHandler>()
        }
    }

    /*
    there are three public methods in this class:
    handleInvocationTargetException - called from AnalyticsService on errors to decide if its a connection error and show
    the no connection screen.
    handleAuthManagerError - called from AuthManager when it can't create a login handler, which means there is a critical connection issue.
    it will show the no connection screen so that user is aware there is a connection issue.
    resetConnectionLostAndNotifyIfNecessary - called both by AnalyticsService and AuthManager on successful connection.

    TODO: what is AuthManager local login always fails?  user will never know about it
     */


    //this errorReportingHelper is used to keep track of errors for helping with reporting messages only when necessary
    // and keep the log and intellij notification panel clean
    private val errorReportingHelper = ErrorReportingHelper()

    private val myLock = ReentrantLock()

    private val myConnectionLostFlag = AtomicBoolean(false)

    //sometimes the connection lost is momentary or regaining is momentary, use the alarm to wait
    // before notifying listeners of connectionLost/ConnectionGained
    private val myConnectionStatusNotifyAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

    //log connection exceptions only the first time and show an error notification.
    // while status is in error, following connection exceptions will not be logged, other exceptions
    // will be logged only once.
    fun handleInvocationTargetException(
        project: Project,
        invocationTargetException: InvocationTargetException,
        method: Method,
        args: Array<Any?>?
    ) {
        try {
            myLock.lock()
            handleInvocationTargetExceptionImpl(project, invocationTargetException, method, args)
        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "error in handleInvocationTargetException {}", e)
            ErrorReporter.getInstance().reportError("ApiErrorHandler.handleInvocationTargetException", e)
        } finally {
            if (myLock.isHeldByCurrentThread) {
                myLock.unlock()
            }
        }
    }


    private fun handleInvocationTargetExceptionImpl(
        project: Project,
        invocationTargetException: InvocationTargetException,
        method: Method,
        args: Array<Any?>?
    ) {

        val connectException =
            findConnectException(invocationTargetException) ?: findSslException(invocationTargetException)

        val isConnectionException = connectException != null
        val message = connectException?.message ?: getNonEmptyMessage(invocationTargetException)


        findActiveProject()?.let { project ->
            ErrorReporter.getInstance().reportAnalyticsServiceError(
                project,
                "AnalyticsInvocationHandler.invoke",
                method.name,
                invocationTargetException,
                isConnectionException
            )
        }

        if (isConnectionOK()) {
            //if more than one thread enter this section the worst that will happen is that we
            // report the error more than once but connectionLost will be fired once because
            // markConnectionLostAndNotify locks, marks and notifies only if connection ok.
            if (isConnectionException) {
                markConnectionLostAndNotify()
                errorReportingHelper.addIfNewError(invocationTargetException)
                Log.warnWithException(
                    logger,
                    invocationTargetException,
                    "Connection exception: error invoking AnalyticsProvider.{}({}), exception {}",
                    method.name,
                    argsToString(args),
                    message
                )
                doForAllProjects { project ->
                    EDT.ensureEDT {
                        NotificationUtil.notifyWarning(
                            project, "<html>Connection error with Digma backend api for method " + method.name + ".<br> "
                                    + message + ".<br> See logs for details."
                        )
                    }
                }

            } else {
                Log.warnWithException(
                    logger,
                    invocationTargetException,
                    "Error invoking AnalyticsProvider.{}({}), exception {}",
                    method.name,
                    argsToString(args),
                    message
                )
                if (errorReportingHelper.addIfNewError(invocationTargetException)) {
                    doForAllProjects { project ->
                        EDT.ensureEDT {
                            NotificationUtil.notifyWarning(
                                project, "<html>Error with Digma backend api for method " + method.name + ".<br> "
                                        + message + ".<br> See logs for details."
                            )
                        }
                    }

                    if (isEOFException(invocationTargetException)) {
                        doForAllProjects { project ->
                            EDT.ensureEDT {
                                NotificationUtil.showBalloonWarning(project, "Digma API EOF error: $message")
                            }
                        }
                    }
                }
            }
        } else {


            //a project is opened after connection is already marked lost and the project doesn't know about it.
            // notify the project that there is no connection
            if (BackendConnectionMonitor.getInstance(project).isConnectionOk()) {
                notifyBackendConnectionMonitorConnectionLost(project)
                fireConnectionLost(project)
            }

            //connection is not ok, marked lost
            if (errorReportingHelper.addIfNewError(invocationTargetException)) {
                Log.warnWithException(
                    logger,
                    invocationTargetException,
                    "New Error invoking AnalyticsProvider.{}({}), exception {}",
                    method.name,
                    argsToString(args),
                    message
                )
            }
        }
    }


    //AuthManager doesn't always have a project, only on startup when calling withAuth
    fun handleAuthManagerError(throwable: Throwable, project: Project?) {
        try {
            myLock.lock()
            handleAuthManagerErrorImpl(throwable, project)
        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "error in handleAuthManagerError {}", e)
            ErrorReporter.getInstance().reportError("ApiErrorHandler.handleAuthManagerError", e)
        } finally {
            if (myLock.isHeldByCurrentThread) {
                myLock.unlock()
            }
        }
    }


    //called from auth manager when it can't create a login handler which is a critical error.
    // we want to show no connection anyway to notify the user that we can't login or refresh the token. there is no way for
    // us out of it so user must know about it.
    //it's not like handleInvocationTargetException from AnalyticsService which may or may not be a connection issue and next call
    // may succeed. without login we can't do anything.
    //the user will see the no connection screen and can click refresh, on refresh we call AuthManager to try
    // login or refresh again and if it will succeed the connection will be refreshed.
    private fun handleAuthManagerErrorImpl(throwable: Throwable, project: Project?) {
        Log.warnWithException(logger, throwable, "got AuthManager error {}", throwable)
        Log.log(logger::warn, "showing no connection on AuthManager error {}", throwable)
        markConnectionLostAndNotify()

        //a project is opened after connection is already marked lost and the project doesn't know about it.
        // notify the project that there is no connection
        project?.let {
            if (BackendConnectionMonitor.getInstance(it).isConnectionOk()) {
                notifyBackendConnectionMonitorConnectionLost(it)
                fireConnectionLost(it)
            }
        }
    }


    //must run in a lock
    private fun markConnectionLostAndNotify() {

        Log.log(logger::warn, "markConnectionLostAndNotify called")

        //this is the second critical section of the race condition,
        // we are in error state so the performance penalty of locking is insignificant.
        //only mark and fire the event if connection is ok, avoid firing the event more than once.
        if (isConnectionOK()) {
            Log.log(logger::warn, "marking connection lost")
            myConnectionLostFlag.set(true)

            //must notify BackendConnectionMonitor immediately and not on background thread, the main reason is
            // that on startup it must be notified immediately before starting to create UI components
            // it will also catch the connection lost event later
            notifyBackendConnectionMonitorConnectionLost()
            registerConnectionLostEvent()

            //wait a second because maybe the connection lost is momentary, and it will be back very soon
            myConnectionStatusNotifyAlarm.cancelAllRequests()
            myConnectionStatusNotifyAlarm
                .addRequest({
                    Log.log(logger::warn, "notifying connectionLost")
                    fireConnectionLost()
                }, 2000)
        }

    }


    //this method should be very fast when the connection is ok
    fun resetConnectionLostAndNotifyIfNecessary(project: Project?) {

        Log.log(logger::trace, "resetConnectionLostAndNotifyIfNecessary called")
        //if connection ok do nothing, will return very fast
        if (isConnectionOK()) {
            Log.log(logger::trace, "resetConnectionLostAndNotifyIfNecessary called, connection ok, nothing to do.")

            //maybe a project was in the process of opening and got connection lost. if the connection was gained very fast
            // maybe some listeners didn't get the connection gained event, notify again
            project?.let {
                if (BackendConnectionMonitor.getInstance(it).isConnectionError()) {
                    notifyBackendConnectionMonitorConnectionGained(it)
                    fireConnectionGained(it)
                }
            }

            return
        }

        try {

            //this is the critical section of the race condition, there is a performance penalty
            // for the locking, but only when recovering from connection lost.
            // the reason for locking here and in markConnectionLostAndNotify is to avoid a situation were myConnectionLostFlag
            // if marked but never reset and to make sure that if we notified connectionLost we will also notify when its gained back.

            myLock.lock()
            resetConnectionLostAndNotifyIfNecessaryImpl()

        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "error in resetConnectionLostAndNotifyIfNecessary {}", e)
            ErrorReporter.getInstance().reportError("ApiErrorHandler.resetConnectionLostAndNotifyIfNecessary", e)
        } finally {
            if (myLock.isHeldByCurrentThread) {
                myLock.unlock()
            }
        }
    }

    private fun resetConnectionLostAndNotifyIfNecessaryImpl() {

        //if connection is ok do nothing.
        if (isConnectionOK()) {
            Log.log(logger::trace, "resetConnectionLostAndNotifyIfNecessary called, connection ok, nothing to do.")
            return
        }

        Log.log(logger::trace, "resetting connection status after connection lost")
        myConnectionLostFlag.set(false)
        errorReportingHelper.reset()
        myConnectionStatusNotifyAlarm.cancelAllRequests()

        notifyBackendConnectionMonitorConnectionGained()
        registerConnectionGainedEvent()

        myConnectionStatusNotifyAlarm.addRequest({
            Log.log(logger::trace, "notifying connectionGained")
            fireConnectionGained()
        }, 1000)

        doForAllProjects { project ->
            EDT.ensureEDT {
                NotificationUtil.showNotification(
                    project,
                    "Digma: Connection reestablished !"
                )
            }
        }
    }


    private fun isConnectionOK(): Boolean {
        return !myConnectionLostFlag.get()
    }


    private fun registerConnectionLostEvent() {
        findActiveProject()?.let { project ->
            ActivityMonitor.getInstance(project).registerConnectionLost()
        }
    }

    private fun registerConnectionGainedEvent() {
        findActiveProject()?.let { project ->
            ActivityMonitor.getInstance(project).registerConnectionGained()
        }
    }

    private fun notifyBackendConnectionMonitorConnectionLost() {
        doForAllProjects { project ->
            notifyBackendConnectionMonitorConnectionLost(project)
        }
    }

    private fun notifyBackendConnectionMonitorConnectionLost(project: Project) {
        BackendConnectionMonitor.getInstance(project).connectionLost()
    }


    private fun notifyBackendConnectionMonitorConnectionGained() {
        doForAllProjects { project ->
            notifyBackendConnectionMonitorConnectionGained(project)
        }
    }

    private fun notifyBackendConnectionMonitorConnectionGained(project: Project) {
        BackendConnectionMonitor.getInstance(project).connectionGained()
    }


    private fun fireConnectionLost() {
        doForAllProjects { project ->
            fireConnectionLost(project)
        }
    }

    private fun fireConnectionLost(project: Project) {
        project.messageBus
            .syncPublisher(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC)
            .connectionLost()
    }


    private fun fireConnectionGained() {
        doForAllProjects { project ->
            fireConnectionGained(project)
        }
    }

    private fun fireConnectionGained(project: Project) {
        project.messageBus
            .syncPublisher(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC)
            .connectionGained()
    }

    private fun doForAllProjects(consumer: Consumer<Project>) {
        ProjectManager.getInstance().openProjects.forEach { project ->
            if (isProjectValid(project)) {
                consumer.accept(project)
            }
        }
    }


    private class ErrorReportingHelper {

        private val errors: MutableSet<String> = HashSet()

        fun reset() {
            errors.clear()
        }


        fun addIfNewError(e: Exception): Boolean {
            val cause = findFirstRealExceptionCause(e)
            val errorName = Objects.requireNonNullElse(cause, e)?.javaClass?.name.toString()
            return errors.add(errorName)
        }

    }

}
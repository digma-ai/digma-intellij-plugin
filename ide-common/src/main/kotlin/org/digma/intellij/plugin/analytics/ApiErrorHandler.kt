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
import org.digma.intellij.plugin.common.ExceptionUtils.findRootCause
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

    private val myLock = ReentrantLock(true)

    private val myConnectionLostFlag = AtomicBoolean(false)

    //sometimes the connection lost is momentary or regaining is momentary, use the alarm to wait
    // before notifying listeners of connectionLost/ConnectionGained
    private val myConnectionStatusNotifyAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)


    fun isNoConnectionMode(): Boolean {
        return myConnectionLostFlag.get()
    }

    fun isConnectionOK(): Boolean {
        return !myConnectionLostFlag.get()
    }

    //log connection exceptions only the first time and show an error notification.
    // while status is in error, following connection exceptions will not be logged, other exceptions
    // will be logged only once.
    fun handleInvocationTargetException(
        project: Project,
        invocationTargetException: InvocationTargetException,
        method: Method,
        args: Array<Any?>?
    ) {
        Log.log(logger::trace, "handleInvocationTargetException called with exception {}", findRootCause(invocationTargetException))
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
            Log.log(logger::trace, "handleInvocationTargetException completed")
        }
    }


    private fun handleInvocationTargetExceptionImpl(
        callerProject: Project,
        invocationTargetException: InvocationTargetException,
        method: Method,
        args: Array<Any?>?
    ) {

        //todo: maybe show no connection on CantConstructClientException

        Log.log(logger::trace, "handleInvocationTargetExceptionImpl called with exception {}", findRootCause(invocationTargetException))

        val connectException =
            findConnectException(invocationTargetException) ?: findSslException(invocationTargetException)

        val isConnectionException = connectException != null
        val message = connectException?.message ?: getNonEmptyMessage(invocationTargetException)


        ErrorReporter.getInstance().reportAnalyticsServiceError(
            callerProject,
            "AnalyticsInvocationHandler.invoke",
            method.name,
            invocationTargetException,
            isConnectionException
        )

        if (isConnectionOK()) {
            Log.log(logger::trace, "connection ok, checking if need to mark connection lost")

            //if more than one thread enter this section the worst that will happen is that we
            // report the error more than once but connectionLost will be fired once because
            // markConnectionLostAndNotify is under lock, marks and notifies only if connection ok.
            if (isConnectionException) {

                Log.log(logger::trace, "error is connection exception, calling markConnectionLostAndNotify")

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

                Log.log(logger::trace, "error is not a connection exception,doing nothing")

                Log.warnWithException(
                    logger,
                    invocationTargetException,
                    "Error invoking AnalyticsProvider.{}({}), exception {}",
                    method.name,
                    argsToString(args),
                    message
                )
                if (errorReportingHelper.addIfNewError(invocationTargetException)) {

                    if (!isErrorToExcludeFromNotifications(invocationTargetException)) {
                        doForAllProjects { project ->
                            EDT.ensureEDT {
                                NotificationUtil.notifyWarning(
                                    project, "<html>Error with Digma backend api for method " + method.name + ".<br> "
                                            + message + ".<br> See logs for details."
                                )
                            }
                        }
                    }
                }
            }
        } else {

            //connection is not ok, marked lost
            Log.log(logger::trace, "already in no connection mode")

            //if the callerProject is opened after connection is already marked lost this project doesn't know about it.
            // notify the project that there is no connection
            if (BackendConnectionMonitor.getInstance(callerProject).isConnectionOk()) {
                notifyBackendConnectionMonitorConnectionLost(callerProject)
                fireConnectionLost(callerProject)
            }


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

    private fun isErrorToExcludeFromNotifications(invocationTargetException: InvocationTargetException): Boolean {
        //add more errors to exclude
        return isEOFException(invocationTargetException)
    }


    //AuthManager doesn't always have a project, only on startup when calling withAuth
    fun handleAuthManagerCantConnectError(throwable: Throwable, project: Project?) {
        Log.log(logger::trace, "handleAuthManagerCantConnectError called with exception {}", throwable)
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
            Log.log(logger::trace, "handleAuthManagerCantConnectError completed")
        }
    }

    fun handleAuthManagerCantRefreshError(throwable: Throwable) {

        Log.log(logger::trace, "handleAuthManagerCantRefreshError called with exception {}", throwable)

        //todo: currently only reporting
        Log.warnWithException(logger, throwable, "error in AuthManager {}", throwable)

        val message = if (throwable is AuthenticationException) {
            throwable.message
        } else {
            getNonEmptyMessage(throwable)
        }

        doForAllProjects { p ->
            EDT.ensureEDT {
                NotificationUtil.notifyWarning(
                    p, "<html>Error with Digma backend " + message + ".<br> "
                            + message + ".<br> See logs for details."
                )
            }
        }
        Log.log(logger::trace, "handleAuthManagerCantRefreshError completed")
    }


    //called from auth manager when it can't create a login handler which is a critical error.
    // we want to show no connection anyway to notify the user that we can't login or refresh the token. there is no way for
    // us out of it so user must know about it.
    //it's not like handleInvocationTargetException from AnalyticsService which may or may not be a connection issue and next call
    // may succeed. without login we can't do anything.
    //the user will see the no connection screen and can click refresh, on refresh we call BackendInfoHolder..refresh() and
    // refreshEnvironments that should refresh the connection mode and trigger loginOrRefresh if the connection is.
    private fun handleAuthManagerErrorImpl(throwable: Throwable, project: Project?) {
        Log.warnWithException(logger, throwable, "got AuthManager error {}", throwable)
        Log.log(logger::trace, "calling markConnectionLostAndNotify on AuthManager error {}", throwable)
        markConnectionLostAndNotify()

        //if a project is opened after connection is already marked lost the project doesn't know about it.
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

        Log.log(logger::trace, "markConnectionLostAndNotify called")

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
                    Log.log(logger::trace, "notifying connectionLost")
                    fireConnectionLost()
                }, 2000)
        } else {
            Log.log(logger::trace, "already in no connection mode")
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
            Log.log(logger::trace, "resetting connection mode after connection lost")
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
        if (isProjectValid(project)) {
            project.messageBus
                .syncPublisher(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC)
                .connectionLost()
        }
    }


    private fun fireConnectionGained() {
        doForAllProjects { project ->
            fireConnectionGained(project)
        }
    }

    private fun fireConnectionGained(project: Project) {
        if (isProjectValid(project)) {
            project.messageBus
                .syncPublisher(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC)
                .connectionGained()
        }
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
            val cause = findRootCause(e)
            val errorName = Objects.requireNonNullElse(cause, e)?.javaClass?.name.toString()
            return errors.add(errorName)
        }

    }

}
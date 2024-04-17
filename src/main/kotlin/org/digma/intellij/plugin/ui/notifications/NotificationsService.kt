package org.digma.intellij.plugin.ui.notifications

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.analytics.NoSelectedEnvironmentException
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.UniqueGeneratedUserId
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.scope.ScopeManager
import org.digma.intellij.plugin.scope.SpanScope
import org.digma.intellij.plugin.ui.toolwindow.ToolWindowIconChanger
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Service(Service.Level.PROJECT)
class NotificationsService(val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private var notificationsButton: NotificationsButton? = null

    private var latestNotificationTime: ZonedDateTime? = null

    override fun dispose() {
        //nothing to do ,used as parent disposable
    }

    fun setBell(notificationsButton: NotificationsButton) {
        this.notificationsButton = notificationsButton
    }


    fun hasUnreadNotifications(): Boolean {

        updateStartTimeIfNecessary()

        return try {
            val unreadResponse = project.service<AnalyticsService>()
                .getUnreadNotificationsCount(service<PersistenceService>().getNotificationsStartDate(), UniqueGeneratedUserId.userId)
            unreadResponse.unreadCount > 0
        } catch (e: NoSelectedEnvironmentException) {
            //just log, it may happen a lot
            Log.debugWithException(logger, project, e, "No selected environment")
            false
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "exception in hasUnreadNotifications")
            ErrorReporter.getInstance().reportError(project, "NotificationsService.hasUnreadNotifications", e)
            false
        }
    }


    fun markAllRead() {

        Backgroundable.executeOnPooledThread {

            try {

                latestNotificationTime?.let {
                    Log.log(logger::trace, project, "marking notifications read with latestNotificationTime {}", it)
                    project.service<AnalyticsService>().setReadNotificationsTime(it.toString(), UniqueGeneratedUserId.userId)
                }

            } catch (e: NoSelectedEnvironmentException) {
                //just log, it may happen a lot
                Log.debugWithException(logger, project, e, "No selected environment")
            } catch (e: AnalyticsServiceException) {
                Log.warnWithException(logger, project, e, "exception in setReadNotificationsTime")
                ErrorReporter.getInstance().reportError(project, "NotificationsService.setReadNotificationsTime", e)
            }

            //make sure the bell updates it state immediately
            notificationsButton?.checkUnread()

            project.service<ToolWindowIconChanger>().changeToRegularIcon()
        }
    }


    fun goToInsight(spanCodeObjectId: String?) {

        EDT.assertNonDispatchThread()

        Log.log(logger::trace, project, "goToInsight called for {}", spanCodeObjectId)

        spanCodeObjectId?.let {
            ScopeManager.getInstance(project).changeScope(SpanScope(spanCodeObjectId))
        }

    }

    fun getNotifications(pageNumber: Int, pageSize: Int, isRead: Boolean): String {

        updateStartTimeIfNecessary()

        //exceptions should be handled in place where calling this method, don't return empty string
        try {
            val notifications = project.service<AnalyticsService>()
                .getNotifications(
                    service<PersistenceService>().getNotificationsStartDate(),
                    UniqueGeneratedUserId.userId,
                    pageNumber,
                    pageSize,
                    isRead
                )
            updateLatestNotificationTime(notifications)
            return notifications
        } catch (e: NoSelectedEnvironmentException) {
            //just log, it may happen a lot
            Log.debugWithException(logger, project, e, "No selected environment")
            throw e
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "exception in getNotifications")
            ErrorReporter.getInstance().reportError(project, "NotificationsService.getNotifications", e)
            throw e
        }
    }

    private fun updateLatestNotificationTime(notifications: String?) {

        //take the date from the first notification , it should be the latest timestamp because the notifications are sorted
        try {
            notifications?.let {
                val objectMapper = ObjectMapper()
                val notificationsArray: ArrayNode = objectMapper.readTree(it).get("notifications") as ArrayNode
                if (notificationsArray.size() > 0) {
                    val timestamp = notificationsArray.get(0).get("timestamp").asText()
                    val latest = ZonedDateTime.parse(timestamp).withZoneSameInstant(ZoneOffset.UTC)
                    if (latestNotificationTime == null || latest.isAfter(latestNotificationTime)) {
                        Log.log(logger::trace, project, "updating latestNotificationTime tp {}", latest)
                        latestNotificationTime = latest
                    }
                }
            }
        } catch (e: Exception) {
            Log.warnWithException(logger, project, e, "exception in updateLatestNotificationTime")
        }
    }

    private fun updateStartTimeIfNecessary() {
        if (service<PersistenceService>().getNotificationsStartDate() == null) {
            service<PersistenceService>().setNotificationsStartDate(
                ZonedDateTime.now().minus(1, ChronoUnit.DAYS).withZoneSameInstant(ZoneOffset.UTC).toString()
            )
        }
    }


    fun resetLatestNotificationTime() {
        //this is a trick. when the popup is hidden we want to markAllRead. the way to catch when popup is hidden is
        // catching the event JBPopupListener.onClosed. but onClose is also called after closing the popup intentionally
        // when clicking view all, when clicking view all we don't want to markAllRead.
        //so nullifying latestNotificationTime will do the trick because if latestNotificationTime is null setReadNotificationsTime will not be called.
        latestNotificationTime = null
    }
}
package org.digma.intellij.plugin.ui.notifications

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.analytics.NoSelectedEnvironmentException
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.UserId
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.MonitoredPanel
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Service(Service.Level.PROJECT)
class NotificationsService(val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    override fun dispose() {
        //nothing to do ,used as parent disposable
    }


    fun hasUnreadNotifications(): Boolean {

        updateStartTimeIfNecessary()

        return try {
            val unreadResponse = project.service<AnalyticsService>()
                .getUnreadNotificationsCount(service<PersistenceService>().state.notificationsStartDate, UserId.userId)
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


    fun setReadNotificationsTime(upToDateTime: String) {
        try {
            project.service<AnalyticsService>().setReadNotificationsTime(upToDateTime, UserId.userId)
        } catch (e: NoSelectedEnvironmentException) {
            //just log, it may happen a lot
            Log.debugWithException(logger, project, e, "No selected environment")
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "exception in setReadNotificationsTime")
            ErrorReporter.getInstance().reportError(project, "NotificationsService.setReadNotificationsTime", e)
        }
    }

    fun goToSpan(spanCodeObjectId: String) {

        EDT.assertNonDispatchThread()

        Log.log(logger::trace, project, "goToSpan called for {}", spanCodeObjectId)
        project.service<ActivityMonitor>().registerSpanLinkClicked(MonitoredPanel.Notifications)
        project.service<InsightsViewOrchestrator>().showInsightsForCodelessSpan(spanCodeObjectId)
    }

    fun getNotifications(pageNumber: Int, pageSize: Int, isRead: Boolean): String {

        updateStartTimeIfNecessary()

        //exceptions should be handled in place where calling this method, don't return empty string
        try {
            return project.service<AnalyticsService>()
                .getNotifications(service<PersistenceService>().state.notificationsStartDate, UserId.userId, pageNumber, pageSize, isRead)
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

    private fun updateStartTimeIfNecessary() {
        if (service<PersistenceService>().state.notificationsStartDate == null) {
            service<PersistenceService>().state.notificationsStartDate =
                ZonedDateTime.now().minus(1, ChronoUnit.DAYS).withZoneSameInstant(ZoneOffset.UTC).toString()
        }
    }


}
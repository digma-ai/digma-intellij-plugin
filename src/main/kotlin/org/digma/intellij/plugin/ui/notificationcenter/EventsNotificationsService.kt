package org.digma.intellij.plugin.ui.notificationcenter

import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import org.digma.intellij.plugin.PluginId
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.common.createObjectMapper
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.common.SpanInfo
import org.digma.intellij.plugin.model.rest.event.CodeObjectEvent
import org.digma.intellij.plugin.model.rest.event.FirstImportantInsightEvent
import org.digma.intellij.plugin.model.rest.event.LatestCodeObjectEventsResponse
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import org.digma.intellij.plugin.scope.ScopeContext
import org.digma.intellij.plugin.scope.ScopeManager
import org.digma.intellij.plugin.scope.SpanScope
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.minutes


const val EVENTS_NOTIFICATION_GROUP = "Digma Events Group"

@Service(Service.Level.PROJECT)
class EventsNotificationsService(val project: Project) : Disposable {

    val objectMapper = createObjectMapper()

    companion object {
        val logger = Logger.getInstance(this::class.java)
    }

    fun waitForEvents() {

        Log.log(logger::info, "starting insights notification service")

        disposingPeriodicTask("EventsNotificationsService.waitForEvents", 1.minutes.inWholeMilliseconds, true) {

            try {

                var lastEventTime = service<PersistenceService>().getLastInsightsEventTime()
                if (lastEventTime == null) {
                    lastEventTime = ZonedDateTime.now().minus(7, ChronoUnit.DAYS).withZoneSameInstant(ZoneOffset.UTC).toString()
                }

                Log.log(logger::trace, "sending getLatestEvents query with lastEventTime={}", lastEventTime)
                val events = AnalyticsService.getInstance(project).getLatestEvents(lastEventTime)
                Log.log(logger::trace, "got latest events {}", events)

                events.events.forEach {
                    when (it) {
                        is FirstImportantInsightEvent -> showNotificationForFirstImportantInsight(it)
                        ////is CodeObjectDurationChangeEvent -> showNotificationForDurationChangeEvent(it)
                    }
                }

                updateLastEventTime(events)

            } catch (e: Throwable) {
                //no need to report connection exception, it is reported by AnalyticsService
                if (ExceptionUtils.isAnyConnectionException(e)) {
                    Log.debugWithException(logger, e, "could not get latest events {}", e)
                } else {
                    Log.warnWithException(logger, e, "could not get latest events {}", e.message)
                    ErrorReporter.getInstance().reportError(project, "EventsNotificationsService.waitForEvents", e)
                }
            }
        }
    }


    private fun updateLastEventTime(events: LatestCodeObjectEventsResponse) {

        if (events.events.isEmpty()) {
            return
        }

        val latest = events.events.maxByOrNull { codeObjectEvent: CodeObjectEvent -> codeObjectEvent.eventRecognitionTime }
        latest?.let {
            service<PersistenceService>().setLastInsightsEventTime(it.eventRecognitionTime.withZoneSameInstant(ZoneOffset.UTC).toString())
            Log.log(logger::trace, "latest event time updated to {}", service<PersistenceService>().getLastInsightsEventTime())
        }
    }


    private fun showNotificationForFirstImportantInsight(importantInsight: FirstImportantInsightEvent) {

        Log.log(logger::info, "got FirstImportantInsightEvent {}", importantInsight)

        var codeObjectId = importantInsight.codeObjectId
        var methodId = importantInsight.codeObjectId
        if (importantInsight.codeObjectId == null) {
            val spanInfoNode = importantInsight.insight.get("spanInfo")
            if (spanInfoNode != null && spanInfoNode is ObjectNode) {
                val spanInfo = objectMapper.treeToValue(spanInfoNode, SpanInfo::class.java)
                codeObjectId = spanInfo.spanCodeObjectId
                methodId = spanInfo.methodCodeObjectId ?: codeObjectId
            }
        }

        if (codeObjectId != null) {

            Log.log(logger::info, "showing Notification For FirstImportantInsight {}", importantInsight)


            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("Show.FirstImportantInsightNotification", mapOf())

            showFirstImportantInsightNotification(
                project,
                codeObjectId,
                methodId,
                importantInsight.environment
            )
        } else {
            Log.log(logger::trace, "Not showing Notification For FirstImportantInsight because codeObjectId is null {}", importantInsight)
        }
    }


//    private fun showNotificationForDurationChangeEvent(durationChangedEvent: CodeObjectDurationChangeEvent) {
//        var codeObjectId = durationChangedEvent.codeObjectId
//        if (durationChangedEvent.codeObjectId == null) {
//            codeObjectId = durationChangedEvent.spanCodeObjectId
//        }
//
//        if (codeObjectId != null) {
//
//            showInsightNotification(
//                project,
//                codeObjectId,
//                codeObjectId,
//                durationChangedEvent.environment
//            )
//
//        }
//    }


    private fun showFirstImportantInsightNotification(project: Project, codeObjectId: String, methodId: String?, environment: String) {

        val notification = NotificationGroupManager.getInstance().getNotificationGroup(EVENTS_NOTIFICATION_GROUP)
            .createNotification("First important insight!", "In analyzing your code Digma found the following insight:", NotificationType.INFORMATION)

        notification.addAction(
            GoToCodeObjectInsightsAction(
                project,
                notification,
                "FirstImportantInsightNotification",
                codeObjectId,
                methodId,
                environment
            )
        )

        notification.setImportant(true)
        notification.setToolWindowId(PluginId.TOOL_WINDOW_ID)

        notification.notify(this.project)
        notification.balloon?.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                notification.expire()
            }
        })
    }

    override fun dispose() {
        //nothing to do
    }


}


class GoToCodeObjectInsightsAction(
    private val project: Project,
    private val notification: Notification,
    private val notificationName: String,
    private val codeObjectId: String,
    private val methodId: String?,
    private val environmentId: String,
) :
    AnAction("Show Insights") {
    override fun actionPerformed(e: AnActionEvent) = try {
        Log.log(EventsNotificationsService.logger::info, "GoToCodeObjectInsightsAction action clicked for {}", codeObjectId)

        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.clicked", mapOf())

        Backgroundable.ensurePooledThreadWithoutReadAccess {
            val scopeContext = ScopeContext("IDE/NOTIFICATION_LINK_CLICKED", null)
            ScopeManager.getInstance(project).changeScope(SpanScope(codeObjectId), false, null, scopeContext, environmentId)
        }

        notification.expire()
    } catch (e: Throwable) {
        ErrorReporter.getInstance().reportError(project, "GoToCodeObjectInsightsAction.actionPerformed", e)
    }

}
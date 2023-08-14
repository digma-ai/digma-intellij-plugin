package org.digma.intellij.plugin.notifications

import com.intellij.collaboration.async.DisposingScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.event.CodeObjectDurationChangeEvent
import org.digma.intellij.plugin.model.rest.event.CodeObjectEvent
import org.digma.intellij.plugin.model.rest.event.FirstImportantInsightEvent
import org.digma.intellij.plugin.model.rest.event.LatestCodeObjectEventsResponse
import org.digma.intellij.plugin.model.rest.insights.SpanInsight
import org.digma.intellij.plugin.navigation.HomeSwitcherService
import org.digma.intellij.plugin.navigation.InsightsAndErrorsTabsHelper
import org.digma.intellij.plugin.navigation.codenavigation.CodeNavigator
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit


const val EVENTS_NOTIFICATION_GROUP = "Digma Events Group"

@Service(Service.Level.PROJECT)
class InsightsNotificationsService(val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    fun waitForEvents() {

        Log.log(logger::info, "starting insights notification service")

        @Suppress("UnstableApiUsage")
        DisposingScope(this).launch {

            while (true) {

                delay(60000)

                try {

                    var lastEventTime = PersistenceService.getInstance().state.lastInsightsEventTime
                    if (lastEventTime == null) {
                        lastEventTime = ZonedDateTime.now().minus(7, ChronoUnit.DAYS).withZoneSameInstant(ZoneOffset.UTC).toString()
                    }

                    val events = project.service<AnalyticsService>().getLatestEvents(lastEventTime)

                    events.events.forEach {
                        when (it) {
                            is FirstImportantInsightEvent -> showNotificationForFirstImportantInsight(it)
//                            is CodeObjectDurationChangeEvent -> showNotificationForDurationChangeEvent(it)
                        }
                    }

                    updateLastEventTime(events)

                } catch (e: Exception) {
                    Log.log(logger::warn, "could not get latest events {}", e.message)
                }
            }
        }
    }


    private fun updateLastEventTime(events: LatestCodeObjectEventsResponse) {

        if (events.events.isEmpty()){
            return
        }

        val latest = events.events.maxByOrNull { codeObjectEvent: CodeObjectEvent -> codeObjectEvent.eventRecognitionTime }
        latest?.let {
            PersistenceService.getInstance().state.lastInsightsEventTime = it.eventRecognitionTime.withZoneSameInstant(ZoneOffset.UTC).toString()
        }
    }


    private fun showNotificationForFirstImportantInsight(importantInsight: FirstImportantInsightEvent) {
        var codeObjectId = importantInsight.codeObjectId
        var methodId = importantInsight.codeObjectId
        if (importantInsight.codeObjectId == null && importantInsight.insight is SpanInsight) {
            codeObjectId = (importantInsight.insight as SpanInsight).spanInfo.spanCodeObjectId
            methodId = (importantInsight.insight as SpanInsight).spanInfo.methodCodeObjectId
        }

        if (codeObjectId != null) {

            showInsightNotification(
                project,
                codeObjectId,
                methodId,
                importantInsight.environment
            )

        }
    }


    private fun showNotificationForDurationChangeEvent(durationChangedEvent: CodeObjectDurationChangeEvent) {
        var codeObjectId = durationChangedEvent.codeObjectId
        if (durationChangedEvent.codeObjectId == null) {
            codeObjectId = durationChangedEvent.spanCodeObjectId
        }

        if (codeObjectId != null) {

            showInsightNotification(
                project,
                codeObjectId,
                codeObjectId,
                durationChangedEvent.environment
            )

        }
    }


    private fun showInsightNotification(project: Project, codeObjectId: String, methodId: String?, environment: String) {


        val notification = NotificationGroupManager.getInstance().getNotificationGroup(EVENTS_NOTIFICATION_GROUP)
            .createNotification("First important insight!", "In analyzing your code Digma found the following insight:", NotificationType.INFORMATION)

        notification.addAction(GoToCodeObjectInsightsAction(project,notification, codeObjectId, methodId, environment))

        notification.notify(this.project)

    }

    override fun dispose() {
        //nothing to do
    }


}


class GoToCodeObjectInsightsAction(
    val project: Project,
    val notification: Notification,
    val codeObjectId: String,
    val methodId: String?,
    val environment: String
) :
    AnAction("Show Insights") {
    override fun actionPerformed(e: AnActionEvent) {

        val canNavigate = project.service<CodeNavigator>().canNavigateToSpanOrMethod(codeObjectId, methodId)

        val environmentsSupplier: EnvironmentsSupplier = project.service<AnalyticsService>().environment

        val runnable = Runnable {
            project.service<HomeSwitcherService>().switchToInsights()
            project.service<InsightsAndErrorsTabsHelper>().switchToInsightsTab()
            if (canNavigate) {
                project.service<InsightsViewOrchestrator>()
                    .showInsightsForSpanOrMethodAndNavigateToCode(codeObjectId, methodId)
            } else {
                project.service<InsightsViewOrchestrator>()
                    .showInsightsForCodelessSpan(codeObjectId)
            }
        }


        if (environmentsSupplier.getCurrent() != environment) {

            environmentsSupplier.setCurrent(environment, false) {
                EDT.ensureEDT {
                    runnable.run()
                }
            }

        } else {
            EDT.ensureEDT {
                runnable.run()
            }
        }

        notification.expire()

    }

}
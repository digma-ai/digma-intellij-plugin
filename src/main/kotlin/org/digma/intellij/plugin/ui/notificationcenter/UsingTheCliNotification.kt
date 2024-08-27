package org.digma.intellij.plugin.ui.notificationcenter

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.digma.intellij.plugin.PluginId
import org.digma.intellij.plugin.activation.UserActivationService
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.scheduling.disposingOneShotDelayedTask
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import java.time.Instant
import kotlin.math.max
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.toDuration

//parentDisposable is the service
fun startUsingTheCliNotificationTimer(parentDisposable: Disposable) {

    //todo: check if any app instrumented
    if (UserActivationService.getInstance().isAnyUsageReported()) {
        return
    }

    val disposable = Disposer.newDisposable()
    Disposer.register(parentDisposable, disposable)
    disposable.disposingPeriodicTask("UsingTheCliNotificationTimer.waitForFirstConnection", 1.minutes.inWholeMilliseconds, true) {
        Log.log(AppNotificationCenter.logger::info, "Starting UsingTheCliNotificationTimer")
        val firstConnectionTime = service<PersistenceService>().getFirstTimeConnectionEstablishedTimestamp()
        if (firstConnectionTime != null) {
            Log.log(AppNotificationCenter.logger::info, "in UsingTheCliNotificationTimer, got firstConnectionTime {}", firstConnectionTime)
            //will cancel this task
            Disposer.dispose(disposable)
            scheduleShowNotification(parentDisposable, firstConnectionTime)
        }
    }
}

private fun scheduleShowNotification(parentDisposable: Disposable, firstConnectionTime: Instant) {

    Log.log(
        AppNotificationCenter.logger::info,
        "in UsingTheCliNotificationTimer, waiting 8 minutes after firstConnectionTime {}",
        firstConnectionTime
    )


    val timeSinceFirstConnection = (Instant.now().toEpochMilli() - firstConnectionTime.toEpochMilli()).toDuration(DurationUnit.MILLISECONDS)
    val delay = max(1.minutes.inWholeMilliseconds, (8.minutes.inWholeMilliseconds - timeSinceFirstConnection.inWholeMilliseconds))

    parentDisposable.disposingOneShotDelayedTask("UsingTheCliNotificationTimer.showUsingTheCliNotification", delay) {
        if (!UserActivationService.getInstance().isAnyUsageReported()) {
            Log.log(AppNotificationCenter.logger::info, "in UsingTheCliNotificationTimer, showing notification")
            showUsingTheCliNotification()
        } else {
            Log.log(
                AppNotificationCenter.logger::info,
                "in UsingTheCliNotificationTimer, not showing notification because usage already reported"
            )
        }
    }
}


private fun showUsingTheCliNotification() {

    //if a project was not found there is no notification
    findActiveProject()?.let { project ->

        val notificationName = "UsingTheCliNotification"

        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("Show.$notificationName", mapOf())

        val notification = NotificationGroupManager.getInstance().getNotificationGroup(STICKY_REMINDERS_NOTIFICATION_GROUP)
            .createNotification(
                "Run your code to use Digma",
                "Using the CLI to run your code? Here is how to connect to Digma",
                NotificationType.INFORMATION
            )

        notification.addAction(OpenUsingTheCliDocumentationAction(project, notification, notificationName))

        notification.whenExpired {
            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.expired", mapOf())
            Log.log(AppNotificationCenter.logger::info, "in UsingTheCliNotification, notification expired")
            service<PersistenceService>().setUsingTheCliNotificationPassed()
        }
        notification.setImportant(true)
        notification.setToolWindowId(PluginId.TOOL_WINDOW_ID)

        notification.notify(project)
    }
}


private class OpenUsingTheCliDocumentationAction(
    private val project: Project,
    private val notification: Notification,
    private val notificationName: String
) : AnAction("Using The CLI") {
    override fun actionPerformed(e: AnActionEvent) {

        try {
            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.clicked", mapOf())

            BrowserUtil.open(
                "https://docs.digma.ai/digma-developer-guide/instrumentation/spring-spring-boot-dropwizard-and-default/instrumenting-code-running-in-cli"
            )

            notification.expire()
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "OpenUsingTheCliDocumentationAction.actionPerformed", e)
        }
    }
}
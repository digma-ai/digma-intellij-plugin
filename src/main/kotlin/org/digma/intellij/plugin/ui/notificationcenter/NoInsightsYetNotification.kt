package org.digma.intellij.plugin.ui.notificationcenter

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Disposer
import org.digma.intellij.plugin.PluginId
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.scheduling.disposingOneShotDelayedTask
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import java.time.Instant
import kotlin.time.Duration.Companion.minutes


fun startNoInsightsYetNotificationTimer(parentDisposable: Disposable) {

    if (service<PersistenceService>().isFirstInsightReceived()) {
        return
    }

    val disposable = Disposer.newDisposable()
    Disposer.register(parentDisposable, disposable)
    disposable.disposingPeriodicTask("NoInsightsYetNotificationTimer.waitForFirstConnection", 1.minutes.inWholeMilliseconds, true) {
        Log.log(AppNotificationCenter.logger::info, "Starting NoInsightsYetNotificationTimer")
        val firstConnectionTime = service<PersistenceService>().getFirstTimeConnectionEstablishedTimestamp()
        if (firstConnectionTime != null) {
            Log.log(AppNotificationCenter.logger::info, "in NoInsightsYetNotificationTimer, got firstConnectionTime {}", firstConnectionTime)
            //will cancel this task
            Disposer.dispose(disposable)
            scheduleShowNotification(parentDisposable, firstConnectionTime)
        }
    }
}

private fun scheduleShowNotification(parentDisposable: Disposable, firstConnectionTime: Instant) {

    Log.log(
        AppNotificationCenter.logger::info,
        "in NoInsightsYetNotificationTimer, waiting 30 minutes after firstConnectionTime {}",
        firstConnectionTime
    )

    val disposable = Disposer.newDisposable()
    Disposer.register(parentDisposable, disposable)
    disposable.disposingOneShotDelayedTask("NoInsightsYetNotificationTimer.showNoInsightsYetNotification", 30.minutes.inWholeMilliseconds) {
        if (!service<PersistenceService>().isFirstInsightReceived()) {
            Log.log(AppNotificationCenter.logger::info, "in NoInsightsYetNotificationTimer, showing notification")
            showNoInsightsYetNotification()
        } else {
            Log.log(
                AppNotificationCenter.logger::info,
                "in NoInsightsYetNotificationTimer, not showing notification because firstTimeInsightReceived is true"
            )
        }
    }
}


private fun showNoInsightsYetNotification() {

    //if a project was not found there is no notification
    findActiveProject()?.let { project ->

        val notificationName = "NoInsightsYetNotification"

        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("Show.$notificationName", mapOf())

        val notification = NotificationGroupManager.getInstance().getNotificationGroup(STICKY_REMINDERS_NOTIFICATION_GROUP)
            .createNotification("We noticed Digma hasn't received any data yet, do you need help with setup?", NotificationType.INFORMATION)

        notification.addAction(ShowTroubleshootingAction(project, notification, notificationName))

        notification.whenExpired {
            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.expired", mapOf())
            Log.log(AppNotificationCenter.logger::info, "in NoInsightsYetNotification, notification expired")
            service<PersistenceService>().setNoInsightsYetNotificationPassed()
        }
        notification.setImportant(true)
        notification.setToolWindowId(PluginId.TOOL_WINDOW_ID)

        notification.notify(project)
    }
}





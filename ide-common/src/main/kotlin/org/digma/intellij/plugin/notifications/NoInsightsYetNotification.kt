package org.digma.intellij.plugin.notifications

import com.intellij.collaboration.async.DisposingScope
import com.intellij.ide.impl.ProjectUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.IdeFocusManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.PluginId
import org.digma.intellij.plugin.common.DatesUtils
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.awt.Window
import java.time.Instant
import java.time.temporal.ChronoUnit

const val STICKY_REMINDERS_NOTIFICATION_GROUP = "Digma sticky Reminders Group"
const val FADING_REMINDERS_NOTIFICATION_GROUP = "Digma fading Reminders Group"

fun startNoInsightsYetNotificationTimer(parentDisposable: Disposable) {

    if (service<PersistenceService>().state.firstTimeInsightReceived) {
        return
    }


    @Suppress("UnstableApiUsage")
    DisposingScope(parentDisposable).launch {

        Log.log(AppNotificationCenter.logger::info, "Starting tNoInsightsYetNotificationTimer")

        while (service<PersistenceService>().state.firstTimeConnectionEstablishedTimestamp == null) {
            delay(60000)
        }

        val firstConnectionTime = DatesUtils.Instants.stringToInstant(service<PersistenceService>().state.firstTimeConnectionEstablishedTimestamp)
        Log.log(AppNotificationCenter.logger::info, "in NoInsightsYetNotificationTimer, got firstConnectionTime {}", firstConnectionTime)

        val after30Minutes = firstConnectionTime.plus(30, ChronoUnit.MINUTES)

        Log.log(
            AppNotificationCenter.logger::info,
            "in NoInsightsYetNotificationTimer, waiting 30 minutes after firstConnectionTime {}",
            firstConnectionTime
        )
        while (Instant.now().isBefore(after30Minutes)) {
            delay(60000)
        }


        if (!service<PersistenceService>().state.firstTimeInsightReceived) {
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
    findProjectForNotification()?.let { project ->

        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("Show.NoInsightsYetNotification", mapOf())

        val notification = NotificationGroupManager.getInstance().getNotificationGroup(STICKY_REMINDERS_NOTIFICATION_GROUP)
            .createNotification("We noticed Digma hasn't received any data yet, do you need help with setup?", NotificationType.INFORMATION)

        notification.addAction(ShowTroubleshootingAction(project, notification, "NoInsightsYetNotification"))

        notification.whenExpired {
            Log.log(AppNotificationCenter.logger::info, "in NoInsightsYetNotification, notification expired")
            service<PersistenceService>().setNoInsightsYetNotificationPassed()
        }
        notification.setImportant(true)
        notification.setToolWindowId(PluginId.TOOL_WINDOW_ID)

        notification.notify(project)
    }
}


fun findProjectForNotification(): Project? {

    //best effort to find a project for the notification

    var project = ProjectUtil.getActiveProject()
    if (project == null || project.isDisposed) {
        project = IdeFocusManager.getGlobalInstance().lastFocusedFrame?.project
        if (project != null && !project.isDisposed) {
            (IdeFocusManager.getGlobalInstance().lastFocusedFrame as? Window)?.toFront()
        }
    }

    if (project == null || project.isDisposed) {
        project = ProjectManager.getInstance().openProjects.firstOrNull { p -> !p.isDisposed }
    }

    return project

}


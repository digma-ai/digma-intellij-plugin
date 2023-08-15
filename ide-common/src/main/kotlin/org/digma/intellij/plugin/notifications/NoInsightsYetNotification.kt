package org.digma.intellij.plugin.notifications

import com.intellij.collaboration.async.DisposingScope
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.PluginId
import org.digma.intellij.plugin.common.DatesUtils
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.time.Instant
import java.time.temporal.ChronoUnit

const val STICKY_REMINDERS_NOTIFICATION_GROUP = "Digma sticky Reminders Group"
const val FADING_REMINDERS_NOTIFICATION_GROUP = "Digma fading Reminders Group"

fun startNoInsightsYetNotificationTimer(parentDisposable: Disposable){

    if (service<PersistenceService>().state.firstTimeInsightReceived){
        return
    }


    @Suppress("UnstableApiUsage")
    DisposingScope(parentDisposable).launch {

        Log.log(AppNotificationCenter.logger::info,"Starting NoInsightsReminderNotificationTimer")

        while (service<PersistenceService>().state.firstTimeConnectionEstablishedTimestamp == null){
            delay(60000)
        }

        val firstConnectionTime = DatesUtils.Instants.stringToInstant(service<PersistenceService>().state.firstTimeConnectionEstablishedTimestamp)
        Log.log(AppNotificationCenter.logger::info,"in NoInsightsReminderNotificationTimer, got firstConnectionTime {}",firstConnectionTime)

        val after30Minutes  = firstConnectionTime.plus(30, ChronoUnit.MINUTES)

        Log.log(AppNotificationCenter.logger::info,"in NoInsightsReminderNotificationTimer, waiting 30 minutes after firstConnectionTime {}",firstConnectionTime)
        while (Instant.now().isBefore(after30Minutes)){
            delay(60000)
        }


        if (!service<PersistenceService>().state.firstTimeInsightReceived){
            Log.log(AppNotificationCenter.logger::info,"in NoInsightsReminderNotificationTimer, showing notification")
            showNoInsightsReminderNotification(parentDisposable)
        }else{
            Log.log(AppNotificationCenter.logger::info,"in NoInsightsReminderNotificationTimer, not showing notification because firstTimeInsightReceived is true")
        }
    }
}



private fun showNoInsightsReminderNotification(parentDisposable: Disposable) {

    val project = ProjectManager.getInstance().openProjects.firstOrNull { project -> !project.isDisposed }

    project?.let {

        val balloonCloseDisposable = Disposer.newDisposable()
        Disposer.register(parentDisposable,balloonCloseDisposable)


        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("Show.NoInsightsReminderNotification",
            mapOf(
                "project" to project.name
            ))

        val notification = NotificationGroupManager.getInstance().getNotificationGroup(STICKY_REMINDERS_NOTIFICATION_GROUP)
            .createNotification( "We noticed Digma hasn't received any data yet, do you need help with setup?", NotificationType.INFORMATION)

        notification.addAction(ShowTroubleshootingAction(it,notification,"NoInsightsReminderNotification"))

        notification.whenExpired {
            balloonCloseDisposable.dispose()
            Log.log(AppNotificationCenter.logger::info,"in NoInsightsReminderNotificationTimer, notification expired")
            service<PersistenceService>().setNoInsightsYetNotificationPassed()
        }
        notification.setImportant(true)
        notification.setToolWindowId(PluginId.TOOL_WINDOW_ID)

        notification.notify(project)


        //todo: best effort to expire the notification if closed with the X,
        // find a better way, maybe create the balloon
        @Suppress("UnstableApiUsage")
        DisposingScope(balloonCloseDisposable).launch {
            val maxWait = System.currentTimeMillis() + 10000
            while (notification.balloon == null || System.currentTimeMillis() > maxWait) {
                delay(10)
            }

            notification.balloon?.addListener(object : JBPopupListener {
                override fun onClosed(event: LightweightWindowEvent) {
                    notification.expire()
                }
            })
        }

    }

}


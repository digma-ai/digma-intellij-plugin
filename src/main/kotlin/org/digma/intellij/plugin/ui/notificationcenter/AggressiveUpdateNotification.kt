package org.digma.intellij.plugin.ui.notificationcenter

import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.datetime.Clock
import org.digma.intellij.plugin.PluginId
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.NotificationsPersistenceState
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import org.digma.intellij.plugin.settings.InternalFileSettings
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.updates.AggressiveUpdateService
import org.digma.intellij.plugin.updates.CurrentUpdateState
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration


fun startAggressiveUpdateNotificationTimer(
    disposable: Disposable,
    project: Project,
    currentlyShowingAggressiveUpdateNotifications: MutableList<Notification>
) {

    //this disposable is disposed by the calling function when the state changes again
    disposable.disposingPeriodicTask("AggressiveUpdateNotification", 30.minutes.inWholeMilliseconds, true) {

        if (AggressiveUpdateService.getInstance(project).isInUpdateMode()) {
            val nextTime = if (service<NotificationsPersistenceState>().state.aggressiveUpdateLastNotified == null) {
                service<NotificationsPersistenceState>().state.aggressiveUpdateLastNotified = Instant.now()
                service<NotificationsPersistenceState>().state.aggressiveUpdateLastNotified!!
            } else {
                val last = service<NotificationsPersistenceState>().state.aggressiveUpdateLastNotified!!
                val delayMinutes = InternalFileSettings.getAggressiveUpdateNotificationsDelayMinutes(1440)
                last.plus(delayMinutes.minutes.toJavaDuration())
            }

            val delay = nextTime.minusMillis(Clock.System.now().toEpochMilliseconds()).toEpochMilli()
            //it's time to show the notification
            if (delay <= 0L) {
                if (AggressiveUpdateService.getInstance(project).isInUpdateMode()) {
                    AppNotificationCenter.getInstance().clearCurrentlyShowingAggressiveUpdateNotifications()
                    service<NotificationsPersistenceState>().state.aggressiveUpdateLastNotified = Instant.now()
                    showAggressiveUpdateNotification(project, currentlyShowingAggressiveUpdateNotifications)
                }
            }
        }
    }
}


private fun showAggressiveUpdateNotification(project: Project, currentlyShowingNotifications: MutableList<Notification>) {

    val notificationName = "UpdateRequiredNotification"

    val updateState = AggressiveUpdateService.getInstance(project).getUpdateState()
    val content = when (updateState.updateState) {
        CurrentUpdateState.UPDATE_BACKEND -> "Your Digma Engine is too old. please update it now using the update link."
        CurrentUpdateState.UPDATE_PLUGIN -> "Your Digma Plugin is too old. please update it now using the update link."
        CurrentUpdateState.UPDATE_BOTH -> "Your Digma Engine/Plugin is too old. please update it now using the update link."
        CurrentUpdateState.OK -> "Should not be here,its a bug!"
    }

    //check again in case it just changed
    if (!AggressiveUpdateService.getInstance(project).isInUpdateMode()) {
        return
    }

    ActivityMonitor.getInstance(project).registerNotificationCenterEvent("Show.$notificationName", mapOf())

    @Suppress("DialogTitleCapitalization")
    val notification = NotificationGroupManager.getInstance().getNotificationGroup(STICKY_REMINDERS_NOTIFICATION_GROUP)
        .createNotification(
            "Digma Update Required",
            content,
            NotificationType.INFORMATION
        )

    notification.addAction(UpdatedNotificationAction(project, notification, notificationName))

    notification.whenExpired {
        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.expired", mapOf())
        Log.log(AppNotificationCenter.logger::info, "in $notificationName, notification expired")
    }
    notification.setImportant(true)
    notification.setToolWindowId(PluginId.TOOL_WINDOW_ID)

    currentlyShowingNotifications.add(notification)
    notification.notify(project)

}


private class UpdatedNotificationAction(
    private val project: Project,
    private val notification: Notification,
    private val notificationName: String
) : AnAction("Update") {

    override fun actionPerformed(e: AnActionEvent) {

        try {
            Log.log(AppNotificationCenter.logger::info, "in $notificationName, action clicked")

            val updateState = AggressiveUpdateService.getInstance(project).getUpdateState()

            ActivityMonitor.getInstance(project).registerUserAction(
                "force update button clicked", mapOf(
                    "update mode" to updateState.updateState,
                    "source" to "native notification"
                )
            )

            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.clicked", mapOf())

            ToolWindowShower.getInstance(project).showToolWindow()

            //there will be a notification for every open project, a click on one of them should expire all the others.
            // will also expire this notification
            AppNotificationCenter.getInstance().clearCurrentlyShowingAggressiveUpdateNotifications()
            notification.expire()

        } catch (e: Throwable) {
            //use service<ErrorReporter> instead of getInstance because ErrorReporter.getInstance may be paused
            service<ErrorReporter>().reportError(project, "$notificationName.actionPerformed", e)
        }
    }
}
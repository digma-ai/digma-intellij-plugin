package org.digma.intellij.plugin.ui.notificationcenter

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
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.NotificationsPersistenceState
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import org.digma.intellij.plugin.ui.recentactivity.RecentActivityService
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


fun startRequestRegisterTimers(parentDisposable: Disposable) {

    if (isUserRegistered()) {
        return
    }

    Log.log(AppNotificationCenter.logger::info, "starting startRequestRegisterTimers")

    val disposable = Disposer.newDisposable()
    Disposer.register(parentDisposable, disposable)
    //wait one minute after project opens and before showing the message
    disposable.disposingPeriodicTask("RequestUserRegister", 1.minutes.inWholeMilliseconds, 6.hours.inWholeMilliseconds, true) {

        if (isUserRegistered()) {
            //will cancel this task
            Disposer.dispose(disposable)
        } else {

            try {
                if (PersistenceService.getInstance().isFirstTimeAssetsReceived() &&
                    !isUserRegistered() &&
                    daysSinceFirstAsset() > 14000 &&
                    moreThen24HoursSinceLastNotified()
                ) {
                    service<NotificationsPersistenceState>().state.requestRegistrationLastNotified = Instant.now()
                    showRequestRegistrationNotification()
                }

            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError("AppNotificationCenter.startRequestRegisterTimers", e)
            }
        }
    }
}


private fun isUserRegistered(): Boolean {
    return PersistenceService.getInstance().getUserRegistrationEmail() != null
}


fun showRequestRegistrationNotification() {
    findActiveProject()?.let { project ->

        val notificationName = "Register"

        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("Show.$notificationName", mapOf())

        val text = asHtml(
            "Please register to continue using Digma.<br>" +
                    "Digma is free! But we'd appreciate if you register if you love using the product.<br>" +
                    "Click the link below to enter your name and email."
        )

        val notification = NotificationGroupManager.getInstance().getNotificationGroup(STICKY_REMINDERS_NOTIFICATION_GROUP)
            .createNotification(text, NotificationType.INFORMATION)

        notification.addAction(RegisterAction(project, notification, notificationName))

        notification.whenExpired {
            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.expired", mapOf())
            Log.log(AppNotificationCenter.logger::debug, "in $notificationName, notification expired")
        }
        notification.setImportant(true)
        notification.setToolWindowId(PluginId.TOOL_WINDOW_ID)

        notification.notify(project)
    }
}


private fun daysSinceFirstAsset(): Long {
    val firstTimeAssetsReceivedTimestamp = PersistenceService.getInstance().getFirstTimeAssetsReceivedTimestamp()
    return firstTimeAssetsReceivedTimestamp?.until(Instant.now(), ChronoUnit.DAYS) ?: 0
}

private fun moreThen24HoursSinceLastNotified(): Boolean {
    val lastNotifiedTimestamp = service<NotificationsPersistenceState>().state.requestRegistrationLastNotified
    //if null return 25 to catch the first time, it should be non-null after the first check
    val hoursPassed = lastNotifiedTimestamp?.until(Instant.now(), ChronoUnit.HOURS) ?: 25L
    return hoursPassed > 24L
}


class RegisterAction(
    private val project: Project,
    private val notification: Notification,
    private val notificationName: String,
) : AnAction("Register") {
    override fun actionPerformed(e: AnActionEvent) {

        try {
            Log.log(AppNotificationCenter.logger::info, "in RegisterAction, action clicked")

            ActivityMonitor.getInstance(project).registerUserAction(
                "RegisterAction link clicked",
                mapOf(
                    "origin" to notificationName
                )
            )

            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.clicked", mapOf())

            openRegistrationDialog(project)

            notification.expire()
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "ShowTypeformAction.actionPerformed", e)
        }
    }

    private fun openRegistrationDialog(project: Project) {
        //this is called on EDT, start background and release the EDT thread
        Backgroundable.ensurePooledThreadWithoutReadAccess {
            project.service<RecentActivityService>().openRegistrationDialog()
        }
    }

}
package org.digma.intellij.plugin.notifications

import com.intellij.collaboration.async.disposingScope
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.PluginId
import org.digma.intellij.plugin.common.UserId
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.htmleditor.DigmaHTMLEditorProvider
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.NotificationsPersistenceState
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.time.Instant
import java.time.temporal.ChronoUnit


const val BACKEND_HASNT_BEEN_RUNNING_FORM = "BACKEND_HASNT_BEEN_RUNNING_FORM"
const val HASNT_BEEN_OPENED_FORM = "HASNT_BEEN_OPENED_FORM"
const val HASNT_BEEN_ACTIVATED_FORM = "HASNT_BEEN_ACTIVATED_FORM"

fun startIdleUserTimers(parentDisposable: Disposable) {

    Log.log(AppNotificationCenter.logger::info, "starting startIdleUserTimers")

    @Suppress("UnstableApiUsage")
    parentDisposable.disposingScope().launch {

        while (isActive) {

            try {

                delay(60 * 1000 * 60)

                //only show one message at a time
                if (PersistenceService.getInstance().isFirstTimeAssetsReceived() &&
                    backendIdleDays() > 3 &&
                    backendHasntBeenRunningLastNotified() > 7
                ) {
                    service<NotificationsPersistenceState>().state.backendHasntBeenRunningForAWhileLastNotified = Instant.now()
                    showDigmaHasntBeenRunningForAWhile()
                } else if (PersistenceService.getInstance().isFirstTimeAssetsReceived() &&
                    backendIdleDays() <= 1 &&
                    userActionIdleDays() > 3 &&
                    hasntBeenOpenedForAWhileLastNotified() > 7
                ) {
                    service<NotificationsPersistenceState>().state.hasntBeenOpenedForAWhileLastNotified = Instant.now()
                    showDigmaHasntBeenOpenedForAWhile()
                } else if (PersistenceService.getInstance().isFirstTimeConnectionEstablished() &&
                    !PersistenceService.getInstance().isFirstTimeAssetsReceived() &&
                    pluginInstalledDays() > 7 &&
                    hasntBeenActivatedLastNotified() > 7
                ) {
                    service<NotificationsPersistenceState>().state.hasntBeenActivatedLastNotified = Instant.now()
                    showDigmaHasntBeenActivated()
                }
            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError("AppNotificationCenter.startIdleUserTimers", e)
            }
        }
    }

}


private fun showDigmaHasntBeenRunningForAWhile() {
    showNotification(
        "DigmaHasntBeenRunningForAWhile",
        asHtml("Is this thing on?<br>Hi! Just checking in :) We noticed that Digma hasn't been running for a while...<br>Did you run into any issue?"),
        BACKEND_HASNT_BEEN_RUNNING_FORM
    )
}

private fun showDigmaHasntBeenOpenedForAWhile() {
    showNotification(
        "DigmaHasntBeenOpenedForAWhile",
        asHtml("Hey :) Is Digma working ok?<br>Hi! Just checking in :) We noticed that Digma hasn't been opened for a while...<br>Did you run into any issue?"),
        HASNT_BEEN_OPENED_FORM
    )
}

private fun showDigmaHasntBeenActivated() {
    showNotification(
        "DigmaHasntBeenActivated",
        asHtml("Not a lot's been going on<br>Hi! Just checking in :) We noticed that Digma was never activated.<br>Is there something we can do to improve our support?"),
        HASNT_BEEN_ACTIVATED_FORM
    )
}


private fun showNotification(name: String, text: String, formName: String) {
    //if a project was not found there is no notification
    findActiveProject()?.let { project ->

        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("Show.$name", mapOf())

        val notification = NotificationGroupManager.getInstance().getNotificationGroup(STICKY_REMINDERS_NOTIFICATION_GROUP)
            .createNotification(text, NotificationType.INFORMATION)

        notification.addAction(ShowTypeformAction(project, notification, name, formName))
        notification.addAction(GoAwayAction(project, notification, name))

        notification.whenExpired {
            Log.log(AppNotificationCenter.logger::debug, "in $name, notification expired")
        }
        notification.setImportant(true)
        notification.setToolWindowId(PluginId.TOOL_WINDOW_ID)

        notification.notify(project)
    }
}


private fun backendHasntBeenRunningLastNotified(): Long {
    val backendHasntBeenRunningForAWhile = service<NotificationsPersistenceState>().state.backendHasntBeenRunningForAWhileLastNotified
    return backendHasntBeenRunningForAWhile?.until(Instant.now(), ChronoUnit.DAYS) ?: 8L
}

private fun hasntBeenOpenedForAWhileLastNotified(): Long {
    val hasntBeenOpenedForAWhile = service<NotificationsPersistenceState>().state.hasntBeenOpenedForAWhileLastNotified
    return hasntBeenOpenedForAWhile?.until(Instant.now(), ChronoUnit.DAYS) ?: 8L
}

private fun hasntBeenActivatedLastNotified(): Long {
    val hasntBeenActivated = service<NotificationsPersistenceState>().state.hasntBeenActivatedLastNotified
    return hasntBeenActivated?.until(Instant.now(), ChronoUnit.DAYS) ?: 8L
}


private fun backendIdleDays(): Long {
    val lastConnectionTimestamp = PersistenceService.getInstance().getLastConnectionTimestamp()
    return lastConnectionTimestamp?.until(Instant.now(), ChronoUnit.DAYS) ?: 0
}

private fun pluginInstalledDays(): Long {
    val firstTimePluginLoadedTimestamp = PersistenceService.getInstance().getFirstTimePluginLoadedTimestamp()
    return firstTimePluginLoadedTimestamp?.until(Instant.now(), ChronoUnit.DAYS) ?: 0
}

private fun userActionIdleDays(): Long {
    val lastUserActionTimestamp = PersistenceService.getInstance().getLastUserActionTimestamp()
    return lastUserActionTimestamp?.until(Instant.now(), ChronoUnit.DAYS) ?: 0
}


class ShowTypeformAction(
    private val project: Project,
    private val notification: Notification,
    private val notificationName: String,
    private val formName: String,
) : AnAction("Yes actually") {
    override fun actionPerformed(e: AnActionEvent) {

        try {
            Log.log(AppNotificationCenter.logger::info, "in ShowTypeformAction, action clicked")

            ActivityMonitor.getInstance(project).registerCustomEvent(
                "ShowTypeform link clicked",
                mapOf(
                    "origin" to notificationName
                )
            )

            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.clicked", mapOf())

            openTypeform(project, formName)
            notification.expire()
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "ShowTypeformAction.actionPerformed", e)
        }
    }

    private fun openTypeform(project: Project, formName: String) {
        val url = getUrlByName(formName)
        DigmaHTMLEditorProvider.openEditorWithUrl(project, notificationName, url)
    }

    private fun getUrlByName(formName: String): String {
        return when (formName) {
            BACKEND_HASNT_BEEN_RUNNING_FORM -> "https://digma-ai.typeform.com/to/VvjuMn9D?u=${UserId.userId}"
            HASNT_BEEN_OPENED_FORM -> "https://digma-ai.typeform.com/to/mk6WAYPW?u=${UserId.userId}"
            HASNT_BEEN_ACTIVATED_FORM -> "https://digma-ai.typeform.com/to/bRzcynub?u=${UserId.userId}"
            else -> "https://digma.ai/"
        }
    }
}


class GoAwayAction(
    private val project: Project,
    private val notification: Notification,
    private val notificationName: String,
) : AnAction("Go away") {
    override fun actionPerformed(e: AnActionEvent) {

        try {
            Log.log(AppNotificationCenter.logger::info, "in GoAwayAction, action clicked")

            ActivityMonitor.getInstance(project).registerCustomEvent(
                "GoAway link clicked",
                mapOf(
                    "origin" to notificationName
                )
            )

            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.clicked", mapOf())
            notification.expire()
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "GoAwayAction.actionPerformed", e)
        }
    }
}



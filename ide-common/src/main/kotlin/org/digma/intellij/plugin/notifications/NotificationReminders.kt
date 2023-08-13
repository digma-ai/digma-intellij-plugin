package org.digma.intellij.plugin.notifications

import com.intellij.collaboration.async.DisposingScope
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.ToolWindowShower

const val STICKY_REMINDERS_NOTIFICATION_GROUP = "Digma sticky Reminders Group"
const val FADING_REMINDERS_NOTIFICATION_GROUP = "Digma fading Reminders Group"

fun startNoInsightsReminderNotificationTimer(parentDisposable: Disposable,project: Project){

    if (service<PersistenceService>().state.firstTimeInsightReceived){
        return
    }

    @Suppress("UnstableApiUsage")
    DisposingScope(parentDisposable).launch {
        delay(60 * 1000 * 30)

        if (!service<PersistenceService>().state.firstTimeInsightReceived){
            showNoInsightsReminderNotification(project)
        }
    }
}



private fun showNoInsightsReminderNotification(project: Project) {

    val notification = NotificationGroupManager.getInstance().getNotificationGroup(STICKY_REMINDERS_NOTIFICATION_GROUP)
        .createNotification( "We noticed Digma hasn't received any data yet, do you need help with setup?", NotificationType.INFORMATION)

    notification.addAction(ShowTroubleshootingAction(project,notification))

    notification.notify(project)
}



fun showInstallationInProgressNotification(){
    NotificationGroupManager.getInstance().getNotificationGroup(FADING_REMINDERS_NOTIFICATION_GROUP)
        .createNotification( "Digma installation is still running", NotificationType.INFORMATION)
        .notify(null)
}


fun showInstallationFinishedNotification(project: Project) {

    if (ToolWindowShower.getInstance(project).isToolWindowVisible){
        return
    }

    val notification = NotificationGroupManager.getInstance().getNotificationGroup(STICKY_REMINDERS_NOTIFICATION_GROUP)
        .createNotification( "Digma successfully installed","Please follow the onboarding steps to run your application with Digma", NotificationType.INFORMATION)

    notification.addAction(ShowToolWindowAction(project,notification))

    notification.notify(project)
}





class ShowTroubleshootingAction(
    val project: Project,
    val notification: Notification,
) :
    AnAction("Troubleshooting") {
    override fun actionPerformed(e: AnActionEvent) {
        ToolWindowShower.getInstance(project).showToolWindow()
        MainToolWindowCardsController.getInstance(project).showTroubleshooting()
        notification.expire()
    }
}


class ShowToolWindowAction(
    val project: Project,
    val notification: Notification,
) :
    AnAction("Open Digma") {
    override fun actionPerformed(e: AnActionEvent) {
        ToolWindowShower.getInstance(project).showToolWindow()
        notification.expire()
    }
}
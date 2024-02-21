package org.digma.intellij.plugin.notifications

import com.intellij.notification.Notification
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.ToolWindowShower


class ShowTroubleshootingAction(
    private val project: Project,
    private val notification: Notification,
    private val notificationName: String
) : AnAction("Troubleshooting") {
    override fun actionPerformed(e: AnActionEvent) {

        try {
            Log.log(AppNotificationCenter.logger::info, "in ShowTroubleshootingAction, action clicked")

            ActivityMonitor.getInstance(project).registerCustomEvent(
                "troubleshooting link clicked",
                mapOf(
                    "origin" to notificationName
                )
            )

            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.clicked", mapOf())

            MainToolWindowCardsController.getInstance(project).closeAllNotificationsIfShowing()
            ToolWindowShower.getInstance(project).showToolWindow()
            MainToolWindowCardsController.getInstance(project).showTroubleshooting()
            notification.expire()
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "ShowTroubleshootingAction.actionPerformed", e)
        }
    }
}


class ShowToolWindowAction(
    private val project: Project,
    private val notification: Notification,
    private val notificationName: String
) : AnAction("Open Digma") {
    override fun actionPerformed(e: AnActionEvent) {
        Log.log(AppNotificationCenter.logger::info,"in ShowToolWindowAction, action clicked")

        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.clicked",mapOf())

        ToolWindowShower.getInstance(project).showToolWindow()
        notification.expire()
    }
}







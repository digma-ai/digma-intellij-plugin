package org.digma.intellij.plugin.notifications

import com.intellij.notification.Notification
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
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
        Log.log(AppNotificationCenter.logger::info,"in ShowTroubleshootingAction, action clicked")

        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("ShowTroubleshootingAction.clicked",
            mapOf(
                "project" to project.name,
                "notificationName" to notificationName
            ))

        ToolWindowShower.getInstance(project).showToolWindow()
        MainToolWindowCardsController.getInstance(project).showTroubleshooting()
        notification.expire()
    }
}


class ShowToolWindowAction(
    private val project: Project,
    private val notification: Notification,
    private val notificationName: String
) : AnAction("Open Digma") {
    override fun actionPerformed(e: AnActionEvent) {
        Log.log(AppNotificationCenter.logger::info,"in ShowToolWindowAction, action clicked")

        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("ShowToolWindowAction.clicked",
            mapOf(
                "project" to project.name,
                "notificationName" to notificationName
            ))

        ToolWindowShower.getInstance(project).showToolWindow()
        notification.expire()
    }
}
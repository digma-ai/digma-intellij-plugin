package org.digma.intellij.plugin.activation

import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.PluginId
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.navigation.MainContentViewSwitcher
import org.digma.intellij.plugin.notifications.NotificationUtil.DIGMA_STICKY_BALLOON_NOTIFICATION_GROUP
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.recentactivity.RecentActivityToolWindowShower
import org.digma.intellij.plugin.ui.ToolWindowShower


fun showNewIssueNotification() {

    findActiveProject()?.let { project ->
        val notificationName = "NewIssueFoundNotification"
        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("Show.$notificationName", mapOf())
        val notification = NotificationGroupManager.getInstance().getNotificationGroup(DIGMA_STICKY_BALLOON_NOTIFICATION_GROUP)
            .createNotification(
                "Digma has some initial findings! Click the link bellow to view the issues and get more information.",
                NotificationType.INFORMATION
            )
        notification.addAction(ShowIssuesAction(project, notification, notificationName))
        notification.whenExpired {
            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.expired", mapOf())
        }
        notification.setImportant(true)
        notification.setToolWindowId(PluginId.TOOL_WINDOW_ID)
        notification.notify(project)
    }
}

fun showNewInsightNotification() {
    findActiveProject()?.let { project ->
        val notificationName = "NewInsightFoundNotification"
        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("Show.$notificationName", mapOf())
        val notification = NotificationGroupManager.getInstance().getNotificationGroup(DIGMA_STICKY_BALLOON_NOTIFICATION_GROUP)
            .createNotification(
                "Digma has some initial analytics results. Click the link bellow to see the list of assets and drill into any specific one to see more data.",
                NotificationType.INFORMATION
            )
        notification.addAction(ShowInsightAction(project, notification, notificationName))
        notification.whenExpired {
            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.expired", mapOf())
        }
        notification.setImportant(true)
        notification.setToolWindowId(PluginId.TOOL_WINDOW_ID)
        notification.notify(project)
    }
}

fun showNewAssetNotification() {
    findActiveProject()?.let { project ->
        val notificationName = "NewAssetFoundNotification"
        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("Show.$notificationName", mapOf())
        val notification = NotificationGroupManager.getInstance().getNotificationGroup(DIGMA_STICKY_BALLOON_NOTIFICATION_GROUP)
            .createNotification(
                "First assets discovered! Digma has received some data about your code. Click the link bellow to see the assets view.",
                NotificationType.INFORMATION
            )
        notification.addAction(ShowAssetAction(project, notification, notificationName))
        notification.whenExpired {
            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.expired", mapOf())
        }
        notification.setImportant(true)
        notification.setToolWindowId(PluginId.TOOL_WINDOW_ID)
        notification.notify(project)
    }
}

fun showNewRecentActivityNotification() {
    findActiveProject()?.let { project ->
        val notificationName = "NewRecentActivityFoundNotification"
        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("Show.$notificationName", mapOf())
        val notification = NotificationGroupManager.getInstance().getNotificationGroup(DIGMA_STICKY_BALLOON_NOTIFICATION_GROUP)
            .createNotification(
                "Digma has some initial analytics results. Click here to see the list of assets and drill into any specific one to see more data.",
                NotificationType.INFORMATION
            )
        notification.addAction(ShowRecentActivityAction(project, notification, notificationName))
        notification.whenExpired {
            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.expired", mapOf())
        }
        notification.setImportant(true)
        notification.setToolWindowId(PluginId.TOOL_WINDOW_ID)
        notification.notify(project)
    }
}


private class ShowIssuesAction(
    private val project: Project,
    private val notification: Notification,
    private val notificationName: String
) : AnAction("Show Issues") {
    override fun actionPerformed(e: AnActionEvent) {

        try {
            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.clicked", mapOf())
            ToolWindowShower.getInstance(project).showToolWindow()
            //todo: show the issues tab
            notification.expire()
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "ShowIssuesAction.actionPerformed", e)
        }
    }
}

private class ShowInsightAction(
    private val project: Project,
    private val notification: Notification,
    private val notificationName: String
) : AnAction("Show Insights") {
    override fun actionPerformed(e: AnActionEvent) {

        try {
            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.clicked", mapOf())
            ToolWindowShower.getInstance(project).showToolWindow()
            //todo: show the insights/analytics tab
            notification.expire()
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "ShowInsightAction.actionPerformed", e)
        }
    }
}

private class ShowAssetAction(
    private val project: Project,
    private val notification: Notification,
    private val notificationName: String
) : AnAction("Show Assets") {
    override fun actionPerformed(e: AnActionEvent) {

        try {
            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.clicked", mapOf())
            ToolWindowShower.getInstance(project).showToolWindow()
            //todo: show the assets tab
            notification.expire()
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "ShowAssetAction.actionPerformed", e)
        }
    }
}

private class ShowRecentActivityAction(
    private val project: Project,
    private val notification: Notification,
    private val notificationName: String
) : AnAction("Show Recent Activity") {
    override fun actionPerformed(e: AnActionEvent) {

        try {
            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.clicked", mapOf())
            ToolWindowShower.getInstance(project).showToolWindow()
            MainContentViewSwitcher.getInstance(project).showAssets()
            RecentActivityToolWindowShower.getInstance(project).showToolWindow()
            notification.expire()
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "ShowRecentActivityAction.actionPerformed", e)
        }
    }
}

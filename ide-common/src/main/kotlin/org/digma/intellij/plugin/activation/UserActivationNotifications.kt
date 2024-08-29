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
import org.digma.intellij.plugin.notifications.NotificationUtil.DIGMA_STICKY_BALLOON_NOTIFICATION_GROUP
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.recentactivity.RecentActivityToolWindowShower
import org.digma.intellij.plugin.ui.ToolWindowShower


fun showNewIssueNotification(title: String, uiMessage: String) {

    findActiveProject()?.let { project ->
        val notificationName = "NewIssueFoundNotification"
        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("Show.$notificationName", mapOf())
        val notification = NotificationGroupManager.getInstance().getNotificationGroup(DIGMA_STICKY_BALLOON_NOTIFICATION_GROUP)
            .createNotification(
                title,
                "Digma has some initial findings! Click the link bellow to view the issues and get more information.",
                NotificationType.INFORMATION
            )
        notification.addAction(ShowIssuesAction(project, notification, notificationName, uiMessage))
        notification.whenExpired {
            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.expired", mapOf())
        }
        notification.setImportant(true)
        notification.setToolWindowId(PluginId.TOOL_WINDOW_ID)
        notification.notify(project)
    }
}

fun showNewInsightNotification(title: String, uiMessage: String) {
    findActiveProject()?.let { project ->
        val notificationName = "NewInsightFoundNotification"
        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("Show.$notificationName", mapOf())
        val notification = NotificationGroupManager.getInstance().getNotificationGroup(DIGMA_STICKY_BALLOON_NOTIFICATION_GROUP)
            .createNotification(
                title,
                "Digma has some initial analytics results. Click the link bellow to see the list of assets and drill into any specific one to see more data.",
                NotificationType.INFORMATION
            )
        notification.addAction(ShowAssetAction(project, notification, notificationName, uiMessage))
        notification.whenExpired {
            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.expired", mapOf())
        }
        notification.setImportant(true)
        notification.setToolWindowId(PluginId.TOOL_WINDOW_ID)
        notification.notify(project)
    }
}

fun showNewAssetNotification(title: String, uiMessage: String) {
    findActiveProject()?.let { project ->
        val notificationName = "NewAssetFoundNotification"
        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("Show.$notificationName", mapOf())
        val notification = NotificationGroupManager.getInstance().getNotificationGroup(DIGMA_STICKY_BALLOON_NOTIFICATION_GROUP)
            .createNotification(
                title,
                "First assets discovered! Digma has received some data about your code. Click the link bellow to see the assets view.",
                NotificationType.INFORMATION
            )
        notification.addAction(ShowAssetAction(project, notification, notificationName, uiMessage))
        notification.whenExpired {
            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.expired", mapOf())
        }
        notification.setImportant(true)
        notification.setToolWindowId(PluginId.TOOL_WINDOW_ID)
        notification.notify(project)
    }
}

fun showNewRecentActivityNotification(title: String, uiMessage: String) {
    findActiveProject()?.let { project ->
        val notificationName = "NewRecentActivityFoundNotification"
        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("Show.$notificationName", mapOf())
        val notification = NotificationGroupManager.getInstance().getNotificationGroup(DIGMA_STICKY_BALLOON_NOTIFICATION_GROUP)
            .createNotification(
                title,
                "Digma has some initial analytics results. Click here to see the list of activities and drill into any specific one to see more data.",
                NotificationType.INFORMATION
            )
        notification.addAction(ShowRecentActivityAction(project, notification, notificationName, uiMessage))
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
    private val notificationName: String,
    private val uiMessage: String,
) : AnAction("Show Issues") {
    override fun actionPerformed(e: AnActionEvent) {

        try {
            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.clicked", mapOf())
            ToolWindowShower.getInstance(project).showToolWindow()
            project.messageBus.syncPublisher(UserClickedNotificationEvent.USER_CLICKED_NOTIFICATION_TOPIC).notificationClicked(uiMessage)
            notification.expire()
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "ShowIssuesAction.actionPerformed", e)
        }
    }
}

//we can't show the analytics tab because there is no home for analytics
private class ShowInsightAction(
    private val project: Project,
    private val notification: Notification,
    private val notificationName: String,
    private val uiMessage: String,
) : AnAction("Show Insights") {
    override fun actionPerformed(e: AnActionEvent) {

        try {
            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.clicked", mapOf())
            ToolWindowShower.getInstance(project).showToolWindow()
            project.messageBus.syncPublisher(UserClickedNotificationEvent.USER_CLICKED_NOTIFICATION_TOPIC).notificationClicked(uiMessage)
            notification.expire()
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "ShowInsightAction.actionPerformed", e)
        }
    }
}

private class ShowAssetAction(
    private val project: Project,
    private val notification: Notification,
    private val notificationName: String,
    private val uiMessage: String,
) : AnAction("Show Assets") {
    override fun actionPerformed(e: AnActionEvent) {

        try {
            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.clicked", mapOf())
            ToolWindowShower.getInstance(project).showToolWindow()
            project.messageBus.syncPublisher(UserClickedNotificationEvent.USER_CLICKED_NOTIFICATION_TOPIC).notificationClicked(uiMessage)
            notification.expire()
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "ShowAssetAction.actionPerformed", e)
        }
    }
}

private class ShowRecentActivityAction(
    private val project: Project,
    private val notification: Notification,
    private val notificationName: String,
    private val uiMessage: String,
) : AnAction("Show Recent Activity") {
    override fun actionPerformed(e: AnActionEvent) {

        try {
            ActivityMonitor.getInstance(project).registerNotificationCenterEvent("$notificationName.clicked", mapOf())
            RecentActivityToolWindowShower.getInstance(project).showToolWindow()
            project.messageBus.syncPublisher(UserClickedNotificationEvent.USER_CLICKED_NOTIFICATION_TOPIC).notificationClicked(uiMessage)
            notification.expire()
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "ShowRecentActivityAction.actionPerformed", e)
        }
    }
}

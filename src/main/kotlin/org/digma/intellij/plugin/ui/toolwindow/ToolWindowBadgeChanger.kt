package org.digma.intellij.plugin.ui.toolwindow

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.ui.notifications.NotificationsService

//todo: remove. Its disabled, we don't need that anymore
class ToolWindowBadgeChanger : StartupActivity {

    private var hasUnreadNotifications = false

    override fun runActivity(project: Project) {

        val notificationsService = project.service<NotificationsService>()

        @Suppress("UnstableApiUsage")
        notificationsService.disposingScope().launch {

            while (isActive) {
                checkUnreadNotifications(project)
                delay(10000)
            }
        }
    }


    private fun checkUnreadNotifications(project: Project) {
        try {
            if (BackendConnectionMonitor.getInstance(project).isConnectionOk()) {
                val notificationsService = project.service<NotificationsService>()

                val hasUnread = notificationsService.hasUnreadNotifications()

                if (hasUnread != hasUnreadNotifications) {
                    hasUnreadNotifications = hasUnread
                    if (hasUnreadNotifications) {
                        project.service<ToolWindowIconChanger>().changeToBadgeIcon()
                    } else {
                        project.service<ToolWindowIconChanger>().changeToRegularIcon()
                    }
                }
            }
        } catch (e: Exception) {
            ErrorReporter.getInstance().reportError(project, "ToolWindowBadgeChanger.checkUnreadNotifications", e)
        }
    }
}


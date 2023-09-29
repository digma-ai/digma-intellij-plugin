package org.digma.intellij.plugin.ui.toolwindow

import com.intellij.collaboration.async.DisposingScope
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.BadgeDotProvider
import com.intellij.ui.BadgeIcon
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.PluginId
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.icons.AppIcons
import org.digma.intellij.plugin.ui.notifications.NotificationsService
import javax.swing.Icon

class ToolWindowBadgeChanger : StartupActivity {

    private var hasUnreadNotifications = false

    override fun runActivity(project: Project) {

        val notificationsService = project.service<NotificationsService>()

        @Suppress("UnstableApiUsage")
        DisposingScope(notificationsService).launch {

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


@Service(Service.Level.PROJECT)
class ToolWindowIconChanger(val project: Project) {


    private var toolWindowIcon: Icon? = null
    private var toolWindowBadgeIcon: Icon? = null

    private fun getToolWindow(): ToolWindow? {
        return ToolWindowManager.getInstance(project).getToolWindow(PluginId.TOOL_WINDOW_ID)
    }


    fun changeToBadgeIcon() {

        EDT.ensureEDT {
            val toolWindow = getToolWindow()

            toolWindow?.let { tw ->
                val badgeIcon = getBadgeIcon(tw)
                tw.setIcon(badgeIcon)
            }
        }

    }

    fun changeToRegularIcon() {

        EDT.ensureEDT {
            val toolWindow = getToolWindow()

            toolWindow?.let { tw ->
                val icon = getIcon(tw)
                tw.setIcon(icon)
            }
        }

    }

    private fun getIcon(toolWindow: ToolWindow): Icon {
        if (toolWindowIcon == null) {
            toolWindowIcon = toolWindow.icon
        }

        if (toolWindowIcon == null) {
            toolWindowIcon = AppIcons.TOOL_WINDOW
        }

        return toolWindowIcon!!
    }

    private fun getBadgeIcon(toolWindow: ToolWindow): Icon {

        if (toolWindowBadgeIcon == null) {
            toolWindowBadgeIcon = createBadgeIcon(getIcon(toolWindow))
        }
        return toolWindowBadgeIcon!!
    }

    private fun createBadgeIcon(icon: Icon): Icon {

        //will put the badge at the bottom of the icon
        ////return ExecutionUtil.getIndicator(icon,icon.iconWidth,icon.iconHeight,JBUI.CurrentTheme.IconBadge.ERROR)

        @Suppress("UnstableApiUsage")
        return BadgeIcon(icon, JBUI.CurrentTheme.IconBadge.ERROR, BadgeDotProvider(0.7, 0.2, 1.5 / icon.iconWidth))
    }

}
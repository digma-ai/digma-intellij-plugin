package org.digma.intellij.plugin.ui.notifications

import com.intellij.collaboration.async.DisposingScope
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.BadgeDotProvider
import com.intellij.ui.BadgeIcon
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.ui.common.Laf
import java.awt.Cursor
import javax.swing.Icon
import javax.swing.JButton

class NotificationsButton(val project: Project) : JButton() {

    private val logger: Logger = Logger.getInstance(NotificationsButton::class.java)

    private var hasUnreadNotifications = false

    companion object {

        val iconDark: Icon = Laf.Icons.Common.NotificationsBellDark
        val iconLight: Icon = Laf.Icons.Common.NotificationsBellLight
        val pressedIconDark: Icon = Laf.Icons.Common.NotificationsBellDarkPressed
        val pressedIconLight: Icon = Laf.Icons.Common.NotificationsBellLightPressed
        val badgeIconDark: Icon = createBadgeIcon(Laf.Icons.Common.NotificationsBellDark)
        val badgeIconLight: Icon = createBadgeIcon(Laf.Icons.Common.NotificationsBellLight)
        val pressedBadgeIconDark: Icon = createBadgeIcon(Laf.Icons.Common.NotificationsBellDarkPressed)
        val pressedBadgeIconLight: Icon = createBadgeIcon(Laf.Icons.Common.NotificationsBellLightPressed)

        @Suppress("UnstableApiUsage")
        private fun createBadgeIcon(icon: Icon): Icon {
            return BadgeIcon(icon, JBUI.CurrentTheme.IconBadge.ERROR, object : BadgeDotProvider() {
                override fun getX(): Double = 0.7
                override fun getY(): Double = 0.2
                override fun getRadius(): Double = 3.0 / icon.iconWidth
            })
        }
    }

    init {

        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        border = JBUI.Borders.empty()
        toolTipText = "Show Notifications"
        isOpaque = false
        isContentAreaFilled = false
        isBorderPainted = false
        border = JBUI.Borders.empty()
        background = Laf.Colors.TRANSPARENT

        updateState()

        addActionListener {
            doActionListener()
        }


        @Suppress("UnstableApiUsage")
        DisposingScope(project.service<NotificationsService>()).launch {

            val notificationsService = project.service<NotificationsService>()
            while (isActive) {
                try {
                    delay(10000)

                    val hasUnread = notificationsService.hasUnreadNotifications()

                    if (hasUnread != hasUnreadNotifications) {
                        hasUnreadNotifications = hasUnread
                        updateState()
                    }
                } catch (e: Exception) {
                    Log.warnWithException(logger, project, e, "exception in NotificationsButton refresh loop")
                    ErrorReporter.getInstance().reportError(project, "NotificationsButton.refreshLoop", e)
                }
            }
        }
    }


    private fun doActionListener() {

        try {

            val topNotificationsPanel = TopNotificationsPanel(project)

            val jbPopup = JBPopupFactory.getInstance().createComponentPopupBuilder(topNotificationsPanel, null)
                .setProject(project)
                .setMovable(true)
                .setTitle("Top Notifications")
                .setResizable(true)
                .addListener(object : JBPopupListener {
                    override fun onClosed(event: LightweightWindowEvent) {
                        topNotificationsPanel.dispose()
                    }
                })
                .createPopup()

            //the panel needs to close the popup when clicking close or clicking a link
            topNotificationsPanel.setPopup(jbPopup)

            //the tool window will always be non-null because the button is on the toolwindow,
            // but must test because ToolWindowShower.getToolWindow is nullable
            if (ToolWindowShower.getInstance(project).toolWindow != null) {
                jbPopup.showInCenterOf(ToolWindowShower.getInstance(project).toolWindow!!.component)
            } else {
                jbPopup.showUnderneathOf(this)
            }

        } catch (e: Exception) {
            Log.warnWithException(logger, project, e, "Error in doActionListener")
            ErrorReporter.getInstance().reportError(project, "Error in notification button action", e)
        }
    }


    private fun updateState() {
        if (hasUnreadNotifications) {
            icon = getBadgeIcon()
            pressedIcon = getPressedBadgeIcon()
        } else {
            icon = getMyIcon()
            pressedIcon = getMyPressedIcon()
        }
    }


    private fun getMyIcon(): Icon {
        return if (JBColor.isBright()) iconLight else iconDark
    }

    private fun getMyPressedIcon(): Icon {
        return if (JBColor.isBright()) pressedIconLight else pressedIconDark
    }

    private fun getBadgeIcon(): Icon {
        return if (JBColor.isBright()) badgeIconLight else badgeIconDark
    }

    private fun getPressedBadgeIcon(): Icon {
        return if (JBColor.isBright()) pressedBadgeIconLight else pressedBadgeIconDark
    }


}

//other ways to create badge icon
//        icon = IconWithLiveIndication(getMyIcon())
//        pressedIcon = IconWithLiveIndication(getMyPressedIcon())
//        icon = com.intellij.execution.runners.ExecutionUtil.getIndicator(getMyIcon(),22,22,JBColor.RED)
//        pressedIcon = com.intellij.execution.runners.ExecutionUtil.getIndicator(getMyPressedIcon(),20,20,JBColor.RED)
//        icon = ExecutionUtil.getLiveIndicator(getMyIcon())
//        pressedIcon = ExecutionUtil.getLiveIndicator(getMyPressedIcon())

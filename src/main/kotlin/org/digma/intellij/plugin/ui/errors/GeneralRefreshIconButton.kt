package org.digma.intellij.plugin.ui.errors

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.refreshInsightsTask.RefreshInsightsTaskScheduled
import org.digma.intellij.plugin.ui.common.Laf
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.SwingConstants


internal class GeneralRefreshIconButton(project: Project, icon: Icon) : JButton(icon), Disposable {

    private val analyticsConnectionEventsConnection: MessageBusConnection = project.messageBus.connect()

    init {
        isOpaque = false
        horizontalAlignment = SwingConstants.CENTER
        isContentAreaFilled = false
        isBorderPainted = false
        border = JBUI.Borders.empty()
        isEnabled = true
        background = Laf.Colors.TRANSPARENT
        val hoverBackground = Laf.Colors.LIST_ITEM_BACKGROUND
        val isGeneralRefreshButtonEnabled = AtomicBoolean(true)

        analyticsConnectionEventsConnection.subscribe(
                RefreshInsightsTaskScheduled.REFRESH_INSIGHTS_TASK_TOPIC,
                handler = object : RefreshInsightsTaskScheduled {
                    override fun refreshInsightsTaskStarted() {
                        isGeneralRefreshButtonEnabled.set(false)
                    }

                    override fun refreshInsightsTaskFinished() {
                        isGeneralRefreshButtonEnabled.set(true)
                    }
                }
        )

        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                isEnabled = isGeneralRefreshButtonEnabled.get()
                if (isGeneralRefreshButtonEnabled.get()) {
                    background = hoverBackground
                }
            }

            override fun mouseExited(e: MouseEvent?) {
                background = Laf.Colors.TRANSPARENT
            }

            override fun mousePressed(e: MouseEvent?) {
                isEnabled = isGeneralRefreshButtonEnabled.get()
                background = Laf.Colors.TRANSPARENT
            }

            override fun mouseReleased(e: MouseEvent?) {
                if (isGeneralRefreshButtonEnabled.get()) {
                    background = hoverBackground
                }
                isEnabled = isGeneralRefreshButtonEnabled.get()
            }

            override fun mouseClicked(e: MouseEvent?) {
                isEnabled = isGeneralRefreshButtonEnabled.get()
                background = Laf.Colors.TRANSPARENT
            }
        })
    }

    override fun dispose() {
        analyticsConnectionEventsConnection.dispose()
    }

    override fun paintComponent(g: Graphics) {
        g.color = background
        g.fillRect(0, 0, width, height)
        super.paintComponent(g)
    }
}
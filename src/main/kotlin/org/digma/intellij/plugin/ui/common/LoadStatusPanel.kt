package org.digma.intellij.plugin.ui.common

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.loadstatus.LoadStatusService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.UserActionOrigin
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants


class LoadStatusPanel(val project: Project) : DigmaResettablePanel() {
    private val logger: Logger = Logger.getInstance(this::class.java)

    private var service = project.service<LoadStatusService>()

    val label = JLabel("Please consider deploying Digma ", SwingConstants.LEFT)
    var actionLink = ActionLink("centrally") {
        ActivityMonitor.getInstance(project).registerUserActionWithOrigin("digma overload warning docs link clicked", UserActionOrigin.LoadStatusPanel)
        BrowserUtil.browse(Links.DIGMA_OVERLOAD_WARNING_DOCS_URL, project)
    }

    init {
        service.affectedPanel = this
        isOpaque = false
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        isVisible = false
        buildItemsInPanel()
    }

    private fun buildItemsInPanel() {
        val borderedPanel = JPanel()
        borderedPanel.layout = BoxLayout(borderedPanel, BoxLayout.Y_AXIS)
        borderedPanel.isOpaque = true
        borderedPanel.border = BorderFactory.createLineBorder(Laf.Colors.BLUE_LIGHT_SHADE, 1)

        val contentPanel = JPanel(BorderLayout())
        contentPanel.background = Laf.Colors.EDITOR_BACKGROUND
        contentPanel.isOpaque = true
        contentPanel.border = JBUI.Borders.empty(6, 10)

        val infoIconWrapper = JPanel()
        infoIconWrapper.layout = BoxLayout(infoIconWrapper, BoxLayout.Y_AXIS)
        infoIconWrapper.isOpaque = false
        infoIconWrapper.border = JBUI.Borders.empty(2, 0, 0, 5)
        val infoIcon = JLabel(Laf.Icons.Common.Info)
        infoIconWrapper.add(infoIcon)

        val linesPanel = JPanel(GridLayout(2, 1, 5, 3))
        linesPanel.isOpaque = false

        val line1Panel = JPanel()
        line1Panel.layout = BoxLayout(line1Panel, BoxLayout.X_AXIS)
        line1Panel.isOpaque = false
        line1Panel.add(JLabel(asHtml(spanBold("Digma is overloaded")), SwingConstants.LEFT))

        val line2Panel = JPanel()
        line2Panel.layout = BoxLayout(line2Panel, BoxLayout.X_AXIS)
        line2Panel.isOpaque = false

        line2Panel.add(label)
        line2Panel.add(actionLink)

        linesPanel.add(line1Panel)
        linesPanel.add(line2Panel)

        contentPanel.add(infoIconWrapper, BorderLayout.WEST)
        contentPanel.add(linesPanel, BorderLayout.CENTER)

        val closeButton = JButton("❌")
        closeButton.isOpaque = false
        closeButton.isBorderPainted = false
        closeButton.isContentAreaFilled = false
        closeButton.preferredSize = Dimension(25, 25)

        closeButton.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                closeButton.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            }

            override fun mouseExited(e: MouseEvent?) {
                closeButton.cursor = Cursor.getDefaultCursor()
            }
        })

        closeButton.addActionListener { e ->
            isVisible = false

            Backgroundable.ensurePooledThread {
                if (service.lastLoadStatus.throttlingType == "ExtendedObservability")
                {
                    val analyticsService = project.service<AnalyticsService>()
                    analyticsService.resetThrottlingStatus()
                }
            }
        }

        contentPanel.add(closeButton, BorderLayout.EAST)


        borderedPanel.add(Box.createVerticalStrut(2))
        borderedPanel.add(contentPanel)
        borderedPanel.add(Box.createVerticalStrut(2))
        this.add(borderedPanel)
    }

    override fun reset() {
        if (service.lastLoadStatus.occurredRecently) {
            if (!isVisible) {
                isVisible = true
                ActivityMonitor.getInstance(project).registerLoadWarning(
                    service.lastLoadStatus.description ?: "",
                    service.lastLoadStatus.lastUpdated
                )
            }
            toolTipText = service.lastLoadStatus.description +
                    "<br/>" +
                    "Last occurred at " + service.lastLoadStatus.lastUpdated

            when (service.lastLoadStatus.throttlingType) {
                "ExtendedObservability" -> {
                    label.text = "Extended observability is generating too much data"
                    actionLink.text = ""
                }

                "InMemory" -> {
                    label.text = "Please consider deploying Digma "
                    actionLink.text = "centrally"
                }

                "Kafka" -> {
                    label.text = "Please consider upgrading the centralized environment"
                    actionLink.text = ""
                }

                else -> {
                    // backward compatibility when no throttlingType for the old backend
                    label.text = "Please consider deploying Digma "
                    actionLink.text = "centrally"
                }
            }
        } else {
            isVisible = false
        }
    }
}
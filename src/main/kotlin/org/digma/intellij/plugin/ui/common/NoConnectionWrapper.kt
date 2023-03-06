package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.ui.list.commonListItemPanel
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

class NoConnectionWrapper(private val project: Project, private val panel: DigmaTabPanel) : DigmaTabPanel() {

    private val settingsState: SettingsState = project.getService(SettingsState::class.java)

    companion object {
        const val WRAPPED_PANEL_CARD = "WRAPPED_PANE"
        const val NO_CONNECTION_CARD = "NO_CONNECTION"
    }

    init {
        layout = CardLayout()
        isOpaque = false

        val noConnectionPanel = createNoConnectionPanel()

        add(panel, WRAPPED_PANEL_CARD)
        add(noConnectionPanel, NO_CONNECTION_CARD)
        (layout as CardLayout).addLayoutComponent(panel, WRAPPED_PANEL_CARD)
        (layout as CardLayout).addLayoutComponent(noConnectionPanel, NO_CONNECTION_CARD)
        showCardBasedOnConnectionStatus()
    }

    private fun createNoConnectionPanel(): Component {

        val messageLabel = JLabel(createNoConnectionMessage())

        //replace the label text for any settings change, although we need only on url change.
        settingsState.addChangeListener {
            EDT.ensureEDT{
                messageLabel.text = createNoConnectionMessage()
            }
        }

        val messagePanel = JPanel()
        messagePanel.layout = BorderLayout()
        messagePanel.add(messageLabel, BorderLayout.CENTER)

        val iconPanel = JPanel()
        iconPanel.layout = BorderLayout()
        val iconLabel = JLabel(Laf.Icons.Environment.NO_CONNECTION_ICON)
        val button = ActionLink("Refresh")
        button.addActionListener {
            project.getService(AnalyticsService::class.java).environment.refreshNowOnBackground()
        }
        iconPanel.add(iconLabel, BorderLayout.NORTH)
        iconPanel.add(button, BorderLayout.SOUTH)

        val noConnectionPanel = JPanel()
        noConnectionPanel.layout = BorderLayout()
        noConnectionPanel.add(messagePanel, BorderLayout.CENTER)
        noConnectionPanel.add(iconPanel, BorderLayout.EAST)

        val result = JPanel()
        result.layout = BorderLayout()
        result.add(commonListItemPanel(noConnectionPanel), BorderLayout.NORTH)
        return result
    }

    private fun createNoConnectionMessage(): String {

        val url = settingsState.apiUrl
        val urlSpan = spanBold(url)

        return buildTitleItalicGrayedComment(
            "We're getting no signal here boss...",
            "We're trying to connect with the " +
                    "Digma backend at $urlSpan, " +
                    "but we're not getting anything back. " +
                    "Please make sure Digma is up and running or change the URL from the plugin settings if it isn't the right one.",
            true
        )
    }

    override fun getPreferredFocusableComponent(): JComponent {

        if (project.isDisposed){
            return this;
        }

        val backendConnectionMonitor = project.getService(BackendConnectionMonitor::class.java)
        return if (backendConnectionMonitor.isConnectionError()) {
            this
        } else {
            panel.getPreferredFocusableComponent()
        }
    }

    override fun getPreferredFocusedComponent(): JComponent {
        val backendConnectionMonitor = project.getService(BackendConnectionMonitor::class.java)
        return if (backendConnectionMonitor.isConnectionError()) {
            this
        } else {
            panel.getPreferredFocusedComponent()
        }
    }

    override fun reset() {
        panel.reset()
        showCardBasedOnConnectionStatus()
    }


    private fun showCardBasedOnConnectionStatus() {
        if (project.isDisposed){
            return
        }


        val backendConnectionMonitor = project.getService(BackendConnectionMonitor::class.java)
        if (backendConnectionMonitor.isConnectionError()) {
            (layout as CardLayout).show(this, NO_CONNECTION_CARD)
        } else {
            (layout as CardLayout).show(this, WRAPPED_PANEL_CARD)
        }
    }

}

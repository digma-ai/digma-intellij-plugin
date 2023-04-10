package org.digma.intellij.plugin.ui.common

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.ui.list.commonListItemPanel
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class NoConnectionWrapper(private val project: Project, private val panel: DigmaTabPanel) : DigmaTabPanel() {

    private val logger = Logger.getInstance(SettingsHintPanel::class.java)

    private val settingsState: SettingsState = SettingsState.getInstance()

    companion object {
        const val WRAPPED_PANEL_CARD = "WRAPPED_PANE"
        const val NO_CONNECTION_CARD = "NO_CONNECTION"
        const val DIGMA_DOCKER_APP_URL = "https://open.docker.com/extensions/marketplace?extensionId=digmaai/digma-docker-extension";

    }

    init {
        layout = CardLayout()
        isOpaque = true

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

        val messagePanel = JPanel( BorderLayout())
        messagePanel.isOpaque = false
        messagePanel.add(messageLabel, BorderLayout.CENTER)

        val iconPanel = JPanel(BorderLayout())
        iconPanel.isOpaque = false

        val iconLabel = JLabel(Laf.Icons.Environment.NO_CONNECTION_ICON)
        iconPanel.add(iconLabel, BorderLayout.NORTH)

        val buttonsPanel = JPanel()
        buttonsPanel.layout = BoxLayout(buttonsPanel, BoxLayout.Y_AXIS)
        buttonsPanel.isOpaque = false
        iconPanel.add(buttonsPanel, BorderLayout.SOUTH)

        val refreshButton = ActionLink("Refresh")
        refreshButton.border = JBUI.Borders.emptyTop(10)
        refreshButton.addActionListener {
            project.getService(AnalyticsService::class.java).environment.refreshNowOnBackground()
        }
        buttonsPanel.add(refreshButton)

        val setupButton = ActionLink("Install")
        setupButton.border = JBUI.Borders.emptyTop(10)
        setupButton.addActionListener {
            try {
                ApplicationManager.getApplication().invokeLater {
                    BrowserUtil.browse(DIGMA_DOCKER_APP_URL, project)
                }
            } catch (ex: Exception) {
                Log.log(logger::debug, "exception opening 'Digma Channel' link = {}, message: {}. ", DIGMA_DOCKER_APP_URL, ex.message)
            }        }
        buttonsPanel.add(setupButton)

        val wizardButton = ActionLink("Set up Digma")
        wizardButton.border = JBUI.Borders.emptyTop(10)
        wizardButton.addActionListener{
            ToolWindowShower.getInstance(project).displayInstallationWizard()
        }
        buttonsPanel.add(wizardButton,)


        val noConnectionPanel = JPanel(BorderLayout())
        noConnectionPanel.isOpaque = false
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
            return this
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

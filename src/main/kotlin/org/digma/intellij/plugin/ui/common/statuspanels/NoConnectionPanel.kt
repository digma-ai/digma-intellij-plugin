package org.digma.intellij.plugin.ui.common.statuspanels

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.settings.ProjectSettings
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.Links.DIGMA_DOCKER_APP_URL
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.SwingConstants


fun createNoConnectionPanel(project: Project):JPanel{

    val settingsState = SettingsState.getInstance()

    val mainPanel = JPanel(GridBagLayout())
    mainPanel.isOpaque = false
    mainPanel.border = empty()

    val htmlText = getNoConnectionMessageHtml(settingsState.apiUrl)
    val textPane = createTextPaneWithHtml(htmlText)
    val messagePanel = createCommonEmptyStatePanelWIthIconAndTextPane(getNoConnectionIcon(),textPane)

    settingsState.addChangeListener {
        EDT.ensureEDT{
            val newHtmlText = getNoConnectionMessageHtml(settingsState.apiUrl)
            textPane.text = newHtmlText
        }
    }


    val constraints = GridBagConstraints()
    constraints.gridx = 1
    constraints.gridy = 1
    mainPanel.add(messagePanel,constraints)


    constraints.gridy = 2
    constraints.ipady = 20
    val settingsLink = ActionLink(" settings."){
        ShowSettingsUtil.getInstance().showSettingsDialog(project, ProjectSettings.DISPLAY_NAME)
    }
    settingsLink.horizontalAlignment = SwingConstants.LEADING
    settingsLink.horizontalTextPosition = SwingConstants.LEADING
    settingsLink.border = empty()
    mainPanel.add(settingsLink,constraints)

    constraints.gridy = 3
    val slackPanel = createSlackLinkPanel(project)
    mainPanel.add(slackPanel,constraints)

    constraints.gridy = 4
    val buttonsPanel = JPanel(BorderLayout(20,10))
    val refreshLink = ActionLink("Refresh"){
        AnalyticsService.getInstance(project).environment.refreshNowOnBackground()
    }
    buttonsPanel.add(refreshLink,BorderLayout.WEST)

    val installLink = ActionLink("Install"){
        BrowserUtil.browse(DIGMA_DOCKER_APP_URL, project)
    }
    buttonsPanel.add(installLink,BorderLayout.EAST)

    val setupLink = ActionLink("Set-up Digma"){
        EDT.ensureEDT{
            MainToolWindowCardsController.getInstance(project).showWizard()
            ToolWindowShower.getInstance(project).showToolWindow()
        }
    }
    buttonsPanel.add(setupLink,BorderLayout.CENTER)
    mainPanel.add(buttonsPanel,constraints)

    return wrapWithScrollable(mainPanel)
}

fun getNoConnectionMessageHtml(apiUrl: String): String {

    val title = "No Connection"
    val paragraph = "We're trying to connect with the<br>" +
            "Digma backend at $apiUrl<br>" +
            "but we're not getting anything back.<br>" +
            "Please make sure Digma is up and running<br>" +
            "or change the URL from the plugin"

    return "<html>" +
            "<head>" +
            "<style>" +
            "h3 {text-align: center;}" +
            "p {text-align: center;}" +
            "div {text-align: center;}" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<h3>$title</h3>" +
            "<p>$paragraph</p>" +
            "</body>" +
            "</html>"
}



private fun getNoConnectionIcon(): Icon {
    return if (JBColor.isBright()) {
        Laf.Icons.Common.NoConnectionLight
    } else {
        Laf.Icons.Common.NoConnectionDark
    }
}
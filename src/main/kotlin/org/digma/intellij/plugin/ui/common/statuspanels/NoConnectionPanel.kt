package org.digma.intellij.plugin.ui.common.statuspanels

import com.intellij.openapi.Disposable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.analytics.BackendInfoHolder
import org.digma.intellij.plugin.auth.AuthManager
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.UserActionOrigin
import org.digma.intellij.plugin.scheduling.oneShotTask
import org.digma.intellij.plugin.settings.ProjectSettings
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.list.listBackground
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.time.Duration.Companion.seconds


fun createNoConnectionPanel(project: Project, parentDisposable: Disposable):JPanel{

    val settingsState = SettingsState.getInstance()

    val mainPanel = JPanel(GridBagLayout())
    mainPanel.isOpaque = false
    mainPanel.border = empty()
    mainPanel.background = listBackground()

    val htmlText = getNoConnectionMessageHtml(settingsState.apiUrl)
    val textPane = createTextPaneWithHtml(htmlText)
    val messagePanel = createCommonEmptyStatePanelWIthIconAndTextPane(getNoConnectionIcon(),textPane)
    messagePanel.background = listBackground()

    settingsState.addChangeListener( {
        EDT.ensureEDT{
            val newHtmlText = getNoConnectionMessageHtml(settingsState.apiUrl)
            textPane.text = newHtmlText
        }
    },parentDisposable)


    val constraints = GridBagConstraints()
    constraints.gridx = 1
    constraints.gridy = 1
    mainPanel.add(messagePanel,constraints)


    constraints.gridy = 2
    constraints.ipady = 20
    val settingsLink = ActionLink(" settings."){
        ActivityMonitor.getInstance(project).registerUserActionWithOrigin("settings link clicked", UserActionOrigin.NoConnectionPanel)
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
    buttonsPanel.background = listBackground()
    val refreshLink = ActionLink("Refresh"){
        Backgroundable.executeOnPooledThread {
            ActivityMonitor.getInstance(project).registerUserActionWithOrigin("refresh link clicked", UserActionOrigin.NoConnectionPanel)
            AuthManager.getInstance().loginOrRefresh()
            BackendInfoHolder.getInstance(project).refresh()
        }
    }
    buttonsPanel.add(refreshLink,BorderLayout.WEST)

    val setupLink = ActionLink("Set-up Digma"){
        oneShotTask("", 2.seconds.inWholeMilliseconds) {
            ActivityMonitor.getInstance(project).registerUserActionWithOrigin("setup digma button clicked", UserActionOrigin.NoConnectionPanel)
        }

        EDT.ensureEDT{
            MainToolWindowCardsController.getInstance(project).showWizard(false)
            ToolWindowShower.getInstance(project).showToolWindow()
        }
    }
    buttonsPanel.add(setupLink, BorderLayout.EAST)
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
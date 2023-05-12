package org.digma.intellij.plugin.ui.common

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.emptyInsets
import com.intellij.util.ui.JBUI.insets
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.ui.common.Links.DIGMA_DOCKER_APP_URL
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants


fun createNoConnectionPanel(project: Project):JPanel{

    val settingsState = SettingsState.getInstance()

    val panel = JPanel(GridBagLayout())

    val constraints = GridBagConstraints()

    constraints.gridx = 1
    constraints.gridy = 1
    constraints.gridwidth = 1
    constraints.gridheight = 1
    constraints.anchor = GridBagConstraints.CENTER
    constraints.insets = insets(10, 5)
    val icon = JLabel(getNoConnectionIcon())
    icon.horizontalAlignment = SwingConstants.CENTER
    panel.add(icon,constraints)

    constraints.gridy = 2
    constraints.fill = GridBagConstraints.BOTH
    val noConnectionLabel = JLabel("No Connection")
    boldFonts(noConnectionLabel)
    noConnectionLabel.horizontalAlignment = SwingConstants.CENTER
    panel.add(noConnectionLabel,constraints)

    constraints.insets = emptyInsets()
    constraints.gridy = 3
    addNoConnectionDetailsPart("We're trying to connect with the",panel,constraints)
    constraints.gridy = 4
    val changingLabel = addNoConnectionDetailsPart("Digma backend at ${settingsState.apiUrl}",panel,constraints)
    settingsState.addChangeListener {
        EDT.ensureEDT{
            changingLabel.text = "Digma backend at ${settingsState.apiUrl}"
        }
    }
    constraints.gridy = 5
    addNoConnectionDetailsPart("but we're not getting anything back.",panel,constraints)
    constraints.gridy = 6
    addNoConnectionDetailsPart("Please make sure Digma is up and running",panel,constraints)
    constraints.gridy = 7
//    addNoConnectionDetailsPart("or change the URL from the plugin settings.",panel,constraints)
    val settingsLink = ActionLink("or change the URL from the plugin settings."){
        ShowSettingsUtil.getInstance().showSettingsDialog(project,"Digma Plugin")
    }
    settingsLink.horizontalAlignment = SwingConstants.CENTER
    settingsLink.horizontalTextPosition = SwingConstants.CENTER
    panel.add(settingsLink,constraints)

    constraints.insets = insets(10, 5)
    constraints.gridy = 8
    constraints.fill = GridBagConstraints.NONE
    val slackLinkPanel = JPanel(BorderLayout(10,5))
    slackLinkPanel.add(JLabel(Laf.Icons.Common.Slack), BorderLayout.WEST)
    val slackLink = ActionLink("Join Our Slack Channel for Support"){
        BrowserUtil.browse(Links.DIGMA_SLACK_SUPPORT_CHANNEL, project)
    }
    slackLinkPanel.add(slackLink, BorderLayout.CENTER)
    panel.add(slackLinkPanel,constraints)



    constraints.gridy = 9
    val buttonsPanel = JPanel(BorderLayout(20,10))
    val refreshLink = ActionLink("Refresh"){
        project.getService(AnalyticsService::class.java).environment.refreshNowOnBackground()
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
    panel.add(buttonsPanel,constraints)

    panel.isOpaque = false
    panel.border = JBUI.Borders.empty()

    return panel
}



private fun addNoConnectionDetailsPart(text: String, panel: JPanel, constraints: GridBagConstraints): JLabel{
    val noConnectionPartDetailsLabel = JLabel(asHtml(text))
    noConnectionPartDetailsLabel.horizontalAlignment = SwingConstants.CENTER
    panel.add(noConnectionPartDetailsLabel,constraints)
    return noConnectionPartDetailsLabel
}


private fun getNoConnectionIcon(): Icon {
    return if (JBColor.isBright()) {
        Laf.Icons.Common.NoConnectionLight
    } else {
        Laf.Icons.Common.NoConnectionDark
    }
}
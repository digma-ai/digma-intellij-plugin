package org.digma.intellij.plugin.ui.common.statuspanels

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.emptyInsets
import com.intellij.util.ui.JBUI.insets
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.Links
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.boldFonts
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants


//common to NonSupportedPanel.kt and NoFilePanel.kt
fun createNoFileInEditorPanel(project: Project,mainMessage: String): JPanel {

    val panel = JPanel(GridBagLayout())

    val constraints = GridBagConstraints()

    constraints.gridx = 1
    constraints.gridy = 1
    constraints.gridwidth = 1
    constraints.gridheight = 1
    constraints.anchor = GridBagConstraints.CENTER
    constraints.insets = insets(10, 0)
    val icon = JLabel(getFileIcon())
    icon.horizontalAlignment = SwingConstants.CENTER
    panel.add(icon,constraints)

    constraints.gridy = 2
    val notSupportedLabel = JLabel(mainMessage)
    boldFonts(notSupportedLabel)
    notSupportedLabel.horizontalAlignment = SwingConstants.CENTER
    notSupportedLabel.horizontalTextPosition = SwingConstants.CENTER
    panel.add(notSupportedLabel,constraints)

    constraints.insets = emptyInsets()
    constraints.gridy = 3
    addNotSupportedDetailsPart("Navigate to any code file in your workspace",panel,constraints)
    constraints.gridy = 4
    addNotSupportedDetailsPart("to see runtime data and insights here.",panel,constraints)

    val slackLinkPanel = JPanel(BorderLayout(10,5))
    slackLinkPanel.add(JLabel(Laf.Icons.General.SLACK), BorderLayout.WEST)
    val slackLink = ActionLink("Join Our Slack Channel for Support"){
        BrowserUtil.browse(Links.DIGMA_SLACK_SUPPORT_CHANNEL, project)
    }
    slackLinkPanel.add(slackLink, BorderLayout.CENTER)

    constraints.insets = insets(10, 0)
    constraints.gridy = 5
    panel.add(slackLinkPanel,constraints)

    panel.isOpaque = false
    panel.border = JBUI.Borders.empty()

    return panel
}


private fun addNotSupportedDetailsPart(text: String, panel: JPanel, constraints: GridBagConstraints){
    val notSupportedDetailsLabel = JLabel(asHtml(text))
    notSupportedDetailsLabel.horizontalAlignment = SwingConstants.CENTER
    notSupportedDetailsLabel.horizontalTextPosition = SwingConstants.CENTER
    panel.add(notSupportedDetailsLabel,constraints)
}


private fun getFileIcon(): Icon {
    return if (JBColor.isBright()) {
        Laf.Icons.Common.FileLight
    } else {
        Laf.Icons.Common.FileDark
    }
}
package org.digma.intellij.plugin.ui.common.statuspanels

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.boldFonts
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

fun createPendingInsightsPanel(): JPanel {

    val panel = JPanel(GridBagLayout())

    val constraints = GridBagConstraints()

    constraints.gridx = 1
    constraints.gridy = 1
    constraints.gridwidth = 1
    constraints.gridheight = 1
    constraints.anchor = GridBagConstraints.CENTER
    constraints.insets = JBUI.insets(10, 5)
    val icon = JLabel(getProcessingIcon())
    icon.horizontalAlignment = SwingConstants.CENTER
    panel.add(icon,constraints)

    constraints.gridy = 2
    val processingLabel = JLabel("Processing Insights...")
    boldFonts(processingLabel)
    processingLabel.horizontalAlignment = SwingConstants.CENTER
    processingLabel.horizontalTextPosition = SwingConstants.CENTER
    panel.add(processingLabel,constraints)

    panel.isOpaque = false
    panel.border = JBUI.Borders.empty()

    return panel
}


private fun getProcessingIcon(): Icon {
    return if (JBColor.isBright()) {
        Laf.Icons.Common.ProcessingLight
    } else {
        Laf.Icons.Common.ProcessingDark
    }
}
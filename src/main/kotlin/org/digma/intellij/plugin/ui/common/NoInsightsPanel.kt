package org.digma.intellij.plugin.ui.common

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

fun createNoInsightsPanel(): JPanel {


    val panel = JPanel(GridBagLayout())

    val constraints = GridBagConstraints()

    constraints.gridx = 1
    constraints.gridy = 2
    constraints.gridwidth = 1
    constraints.gridheight = 1
    constraints.anchor = GridBagConstraints.CENTER
    constraints.insets = JBUI.insets(10, 5)
    val icon = JLabel(getIcon())
    icon.horizontalAlignment = SwingConstants.CENTER
    panel.add(icon, constraints)

    constraints.gridx = 1
    constraints.gridy = 3
    constraints.gridwidth = 1
    constraints.gridheight = 1
    constraints.fill = GridBagConstraints.BOTH
    constraints.anchor = GridBagConstraints.CENTER
    constraints.insets = JBUI.insets(10, 5)
    val noInsights = JLabel("No Insights")
    boldFonts(noInsights)
    noInsights.horizontalAlignment = SwingConstants.CENTER
    panel.add(noInsights, constraints)

    panel.isOpaque = false
    panel.border = JBUI.Borders.empty()

    return panel
}



private fun getIcon(): Icon {
    return if (JBColor.isBright()) {
        Laf.Icons.Common.NoInsightsLight
    } else {
        Laf.Icons.Common.NoInsightsDark
    }
}



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


fun createNoInsightsPanel(): JPanel {

    val icon = if (JBColor.isBright()) {
        Laf.Icons.Common.NoInsightsLight
    } else {
        Laf.Icons.Common.NoInsightsDark
    }

    return createSimpleEmptyStatePanel(icon,"No Insights")
}




fun createPendingInsightsPanel(): JPanel {

    val icon = if (JBColor.isBright()) {
        Laf.Icons.Common.ProcessingLight
    } else {
        Laf.Icons.Common.ProcessingDark
    }

    return createSimpleEmptyStatePanel(icon,"Processing Insights...")
}


fun createLoadingInsightsPanel(): JPanel {

    val icon = if (JBColor.isBright()) {
        Laf.Icons.Common.LoadingLight
    } else {
        Laf.Icons.Common.LoadingDark
    }

    return createSimpleEmptyStatePanel(icon,"Loading...")
}




private fun createSimpleEmptyStatePanel(icon:Icon,text: String): JPanel {

    val panel = JPanel(GridBagLayout())
    panel.isOpaque = false
    panel.border = JBUI.Borders.empty()

    val constraints = GridBagConstraints()

    constraints.gridx = 1
    constraints.gridy = 1
    constraints.gridwidth = 1
    constraints.gridheight = 1
    constraints.anchor = GridBagConstraints.CENTER
    constraints.insets = JBUI.insets(10, 5)
    val iconLabel = JLabel(icon)
    iconLabel.horizontalAlignment = SwingConstants.CENTER
    panel.add(iconLabel,constraints)

    constraints.gridy = 2
    val textLabel = JLabel(text)
    boldFonts(textLabel)
    textLabel.horizontalAlignment = SwingConstants.CENTER
    textLabel.horizontalTextPosition = SwingConstants.CENTER
    panel.add(textLabel,constraints)


    return panel
}



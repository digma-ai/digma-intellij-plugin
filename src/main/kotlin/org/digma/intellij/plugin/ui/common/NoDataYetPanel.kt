package org.digma.intellij.plugin.ui.common

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

fun createNoDataYetPanel(): JPanel {

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
    panel.add(icon,constraints)

    constraints.gridx = 1
    constraints.gridy = 3
    constraints.gridwidth = 1
    constraints.gridheight = 1
    constraints.fill = GridBagConstraints.BOTH
    constraints.anchor = GridBagConstraints.CENTER
    constraints.insets = JBUI.insets(10, 5)
    val noObservability = JLabel("No Data Yet")
    boldFonts(noObservability)
    noObservability.horizontalAlignment = SwingConstants.CENTER
    panel.add(noObservability,constraints)

    addNoDataYetDetailsPart("Trigger actions that call this code ",panel,4)
    addNoDataYetDetailsPart("object to learn more about it's ",panel,5)
    addNoDataYetDetailsPart("runtime behavior.",panel,6)
    panel.isOpaque = false
    panel.border = JBUI.Borders.empty()

    return panel
}


private fun addNoDataYetDetailsPart(text: String, panel: JPanel, gridy: Int){
    val constraints = GridBagConstraints()
    constraints.gridx = 1
    constraints.gridy = gridy
    constraints.gridwidth = 1
    constraints.gridheight = 1
    constraints.fill = GridBagConstraints.HORIZONTAL
    constraints.anchor = GridBagConstraints.CENTER
    val noObservabilityDetailsLabel = JLabel(asHtml(text))
    noObservabilityDetailsLabel.horizontalAlignment = SwingConstants.CENTER
    panel.add(noObservabilityDetailsLabel,constraints)
}


private fun getIcon():Icon{
    return if (JBColor.isBright()){
        Laf.Icons.Common.NoDataYetLight
    }else{
        Laf.Icons.Common.NoDataYetDark
    }
}


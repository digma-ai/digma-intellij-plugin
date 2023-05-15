package org.digma.intellij.plugin.ui.common.statuspanels

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.emptyInsets
import com.intellij.util.ui.JBUI.insets
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.boldFonts
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
    constraints.gridy = 1
    constraints.gridwidth = 1
    constraints.gridheight = 1
    constraints.anchor = GridBagConstraints.CENTER
    constraints.insets = insets(10, 5)
    val icon = JLabel(getNoDataYetIcon())
    icon.horizontalAlignment = SwingConstants.CENTER
    panel.add(icon,constraints)

    constraints.gridy = 2
    val noObservability = JLabel("No Data Yet")
    boldFonts(noObservability)
    noObservability.horizontalAlignment = SwingConstants.CENTER
    noObservability.horizontalTextPosition = SwingConstants.CENTER
    panel.add(noObservability,constraints)

    constraints.insets = emptyInsets()
    constraints.gridy = 3
    addNoDataYetDetailsPart("Trigger actions that call this code ",panel,constraints)
    constraints.gridy = 4
    addNoDataYetDetailsPart("object to learn more about it's ",panel,constraints)
    constraints.gridy = 5
    addNoDataYetDetailsPart("runtime behavior.",panel,constraints)

    panel.isOpaque = false
    panel.border = JBUI.Borders.empty()

    return panel
}


private fun addNoDataYetDetailsPart(text: String, panel: JPanel, constraints: GridBagConstraints){
    val noObservabilityDetailsLabel = JLabel(asHtml(text))
    noObservabilityDetailsLabel.horizontalAlignment = SwingConstants.CENTER
    noObservabilityDetailsLabel.horizontalTextPosition = SwingConstants.CENTER
    panel.add(noObservabilityDetailsLabel,constraints)
}


private fun getNoDataYetIcon():Icon{
    return if (JBColor.isBright()){
        Laf.Icons.Common.NoDataYetLight
    }else{
        Laf.Icons.Common.NoDataYetDark
    }
}


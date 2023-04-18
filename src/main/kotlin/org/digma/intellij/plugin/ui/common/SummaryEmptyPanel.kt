package org.digma.intellij.plugin.ui.common

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

fun createSummaryEmptyPanel(): JPanel {

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
    val noObservability = JLabel("Nothing To See Here Just Yet...")
    boldFonts(noObservability)
    noObservability.horizontalAlignment = SwingConstants.CENTER
    panel.add(noObservability,constraints)

    addSummaryEmptyDetailsPart("Come back once we've munched",panel,4)
    addSummaryEmptyDetailsPart("on more data to see some highlights",panel,5)
    addSummaryEmptyDetailsPart("about your code.",panel,6)
    panel.isOpaque = false
    panel.border = JBUI.Borders.empty()

    return panel
}


private fun addSummaryEmptyDetailsPart(text: String, panel: JPanel, gridy: Int){
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
        Laf.Icons.Common.SummaryEmptyLight
    }else{
        Laf.Icons.Common.SummaryEmptyDark
    }
}


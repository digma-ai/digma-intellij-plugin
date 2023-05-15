package org.digma.intellij.plugin.ui.common.statuspanels

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI.Borders.empty
import com.intellij.util.ui.JBUI.emptyInsets
import com.intellij.util.ui.JBUI.insets
import com.intellij.util.ui.JBUI.insetsBottom
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.boldFonts
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

fun createNoErrorsPanel(): JPanel {

    val panel = JPanel(GridBagLayout())

    val constraints = GridBagConstraints()

    constraints.gridx = 1
    constraints.gridy = 1
    constraints.gridwidth = 1
    constraints.gridheight = 1
    constraints.anchor = GridBagConstraints.CENTER
    constraints.insets = insets(10, 5)
    val icon = JLabel(getNoErrorsIcon())
    icon.horizontalAlignment = SwingConstants.CENTER
    panel.add(icon,constraints)

    constraints.gridy = 2
    val goodNewsLabel = JLabel("Good News!")
    boldFonts(goodNewsLabel)
    goodNewsLabel.horizontalAlignment = SwingConstants.CENTER
    goodNewsLabel.horizontalTextPosition = SwingConstants.CENTER
    panel.add(goodNewsLabel,constraints)

    constraints.insets = insetsBottom(10)
    constraints.gridy = 3
    val noErrorsLabel = JLabel("No Errors Where Recorded Here Yet.")
    boldFonts(noErrorsLabel)
    noErrorsLabel.horizontalAlignment = SwingConstants.CENTER
    noErrorsLabel.horizontalTextPosition = SwingConstants.CENTER
    panel.add(noErrorsLabel,constraints)

    constraints.insets = emptyInsets()
    constraints.gridy = 4
    addNoErrorsDetailsPart("You should return to this page if",panel,constraints)
    constraints.gridy = 5
    addNoErrorsDetailsPart("anu exceptions do occur to see",panel,constraints)
    constraints.gridy = 6
    addNoErrorsDetailsPart("more details.",panel,constraints)

    panel.isOpaque = false
    panel.border = empty()

    return panel
}



private fun addNoErrorsDetailsPart(text: String, panel: JPanel, constraints: GridBagConstraints){
    val noObservabilityDetailsLabel = JLabel(asHtml(text))
    noObservabilityDetailsLabel.horizontalAlignment = SwingConstants.CENTER
    noObservabilityDetailsLabel.horizontalTextPosition = SwingConstants.CENTER
    panel.add(noObservabilityDetailsLabel,constraints)
}


private fun getNoErrorsIcon():Icon{
    return if (JBColor.isBright()){
        Laf.Icons.Common.NoErrorsLight
    }else{
        Laf.Icons.Common.NoErrorsDark
    }
}




package org.digma.intellij.plugin.ui.common.statuspanels

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.insets
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.boldFonts
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants


fun createLoadingInsightsPanel(): JPanel {

    val panel = JPanel(GridBagLayout())

    val constraints = GridBagConstraints()

    constraints.gridx = 1
    constraints.gridy = 1
    constraints.gridwidth = 1
    constraints.gridheight = 1
    constraints.anchor = GridBagConstraints.CENTER
    constraints.insets = insets(10, 5)
    val icon = JLabel(getLoadingIcon())
    icon.horizontalAlignment = SwingConstants.CENTER
    panel.add(icon,constraints)

    constraints.gridy = 2
    val loadingLabel = JLabel("Loading...")
    boldFonts(loadingLabel)
    loadingLabel.horizontalAlignment = SwingConstants.CENTER
    panel.add(loadingLabel,constraints)

    panel.isOpaque = false
    panel.border = JBUI.Borders.empty()

    return panel
}


private fun getLoadingIcon(): Icon {
    return if (JBColor.isBright()) {
        Laf.Icons.Common.LoadingLight
    } else {
        Laf.Icons.Common.LoadingDark
    }
}
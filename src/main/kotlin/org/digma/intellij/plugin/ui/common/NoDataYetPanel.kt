package org.digma.intellij.plugin.ui.common

import com.intellij.ui.ColorHexUtil
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.*
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

fun createNoDataYetPanel(): DigmaResettablePanel {

    val panel = JPanel(GridBagLayout())

    val constraints = GridBagConstraints()

    constraints.gridx = 1
    constraints.gridy = 2
    constraints.gridwidth = 1
    constraints.gridheight = 1
    constraints.anchor = GridBagConstraints.CENTER
    constraints.insets = JBUI.insets(10, 5)
    val icon = JLabel(NoDataYetIcon())
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

    val resettablePanel = object: DigmaResettablePanel(){
        override fun reset() {
        }
    }
    resettablePanel.layout = BorderLayout()
    resettablePanel.add(panel, BorderLayout.CENTER)

    return resettablePanel
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



private class NoDataYetIcon: Icon {

    private val icon = Laf.Icons.Common.NoDataYet
    private val w: Int
    private val h: Int
    init {
        val d = sqrt(icon.iconWidth.toDouble().pow(2.0) + icon.iconHeight.toDouble().pow(2.0))
        w = d.roundToInt()
        h = d.roundToInt()
    }


    override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
        val g2d = g as Graphics2D
        g2d.color = ColorHexUtil.fromHex("#323334")
        g2d.fillOval(x,y,iconWidth,iconHeight)
        val iconX = (w - icon.iconWidth) / 2
        val iconY = (h - icon.iconHeight) / 2
        icon.paintIcon(c,g,iconX,iconY)
    }

    override fun getIconWidth(): Int {
        return w
    }

    override fun getIconHeight(): Int {
        return w
    }
}
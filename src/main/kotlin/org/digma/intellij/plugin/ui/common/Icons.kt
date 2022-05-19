package org.digma.intellij.plugin.ui.common

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel


fun iconPanel(icon: Icon, text: String): JPanel {

    val panel = JPanel()
    panel.layout = BorderLayout(5, 20)
    panel.add(JLabel(icon), BorderLayout.CENTER)
    panel.add(JLabel(text), BorderLayout.SOUTH)
    panel.maximumSize = Dimension(48, -1)

    panel.background = Color.BLUE
    return panel
}
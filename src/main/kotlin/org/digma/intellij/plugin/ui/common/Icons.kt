package org.digma.intellij.plugin.ui.common

import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel


fun iconPanel(icon: Icon, text: String): JPanel {

    val panel = JPanel()
    panel.layout = BorderLayout(5, 20)
    panel.add(JLabel(icon), BorderLayout.CENTER)
    panel.add(JLabel("HotSpot"), BorderLayout.SOUTH)
    panel.maximumSize = Dimension(48, -1)

    return panel
}
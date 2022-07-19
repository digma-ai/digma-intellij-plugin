package org.digma.intellij.plugin.ui.list

import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.Laf.panelsListBackground
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JPanel


fun listBackground(): Color {
    return panelsListBackground()
}


//this method is just an option,not necessary to use. but if two lists want to look the same they can both
//wrap the panels with this method. currently the insights and errors lists use it.
fun commonListItemPanel(panel: JPanel): JPanel {
    panel.border = JBUI.Borders.empty(Laf.scaleBorders(2))
    val wrapper = JPanel()
    wrapper.layout = BorderLayout()
    wrapper.add(panel, BorderLayout.CENTER)
    wrapper.border = JBUI.Borders.empty(Laf.scaleBorders(3))
    wrapper.isOpaque = false
    return wrapper
}

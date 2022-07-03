package org.digma.intellij.plugin.ui.list

import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.Laf.panelsListBackground
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Insets
import javax.swing.JPanel


fun listBackground(): Color {
    return panelsListBackground()
}


fun listItemPanel(panel: JPanel): JPanel {
    panel.border = JBUI.Borders.empty(Laf.scaleBorders(2))
    val wrapper = JPanel()
    wrapper.layout = BorderLayout()
    wrapper.add(panel, BorderLayout.CENTER)
    wrapper.border = JBUI.Borders.empty(Laf.scaleBorders(3))
    wrapper.isOpaque = false
    return wrapper
}

fun listGroupPanel(panel: JPanel): JPanel {
    panel.isOpaque = false
    panel.border = JBUI.Borders.empty()
    val wrapper = object: JPanel(){
        override fun getInsets(): Insets {
            return Insets(0,0,0,0)
        }
    }
    wrapper.layout = BorderLayout()
    wrapper.add(panel, BorderLayout.CENTER)
    wrapper.isOpaque = false
    wrapper.border = JBUI.Borders.empty()
    return wrapper
}
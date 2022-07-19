package org.digma.intellij.plugin.ui.list

import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.ui.common.Laf
import java.awt.*
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JPanel


fun listBackground(): Color {
    return Laf.panelsListBackground()
}

//this method is just an option,not necessary to use. but if two lists want to look the same they can both
//wrap the panels with this method. currently the insights and errors lists use it.
fun commonListItemPanel(panel: JPanel): JPanel {
    panel.border = empty(Laf.scaleBorders(5))
    panel.isOpaque = false

    val wrapper = RoundedPanel(7)
    wrapper.layout = BorderLayout()
    wrapper.add(panel, BorderLayout.CENTER)
    wrapper.border = empty(Laf.scaleBorders(0),Laf.scaleBorders(2),Laf.scaleBorders(0),Laf.scaleBorders(2))
    wrapper.background = Laf.Colors.LIST_ITEM_BACKGROUND
    return wrapper
}

class RoundedPanel(val radius: Int) : JPanel() {

    init {
        isOpaque = false
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val graphics = g as Graphics2D
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        graphics.color = super.getBackground()
        val border = super.getBorder().getBorderInsets(this)
        graphics.fillRoundRect(border.left, border.top, width-border.left-border.right, height-border.top-border.bottom, radius, radius)
    }
}
package org.digma.intellij.plugin.ui.list

import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.ui.common.DigmaColors
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.Laf.panelsListBackground
import java.awt.*
import javax.swing.JPanel


fun listBackground(): Color {
    return panelsListBackground()
}


fun listItemPanel(panel: JPanel): JPanel {
    panel.border = empty(Laf.scaleBorders(5))
    panel.isOpaque = false
    //panel.border = JBUI.Borders.empty()

    val wrapper = RoundedPanel(7)
    wrapper.layout = BorderLayout()
    wrapper.add(panel, BorderLayout.CENTER)
    wrapper.border = empty(Laf.scaleBorders(5))
    //wrapper.isOpaque = false
    wrapper.background = DigmaColors.LIST_ITEM_BACKGROUND
    return wrapper
}

fun listGroupPanel(panel: JPanel): JPanel {
    panel.isOpaque = false
    panel.border = empty()
    val wrapper = object: JPanel(){
        override fun getInsets(): Insets {
            return Insets(0,0,0,0)
        }
    }
    wrapper.layout = BorderLayout()
    wrapper.add(panel, BorderLayout.CENTER)
    wrapper.isOpaque = false
    wrapper.border = empty()
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
        graphics.fillRoundRect(border.left, border.top, width-border.right, height-border.bottom, radius, radius)
    }
}
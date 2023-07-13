package org.digma.intellij.plugin.ui

import com.intellij.util.ui.JBUI
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent


open class Circle : JComponent() {

    init {
        isOpaque = false
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val border = super.getBorder()?.getBorderInsets(this)?: JBUI.emptyInsets()
        val graphics = g as Graphics2D
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.color = super.getBackground()
        graphics.fillOval(border.left, border.top, width-border.left-border.right, height-border.top-border.bottom)
    }
}
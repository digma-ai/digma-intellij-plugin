package org.digma.intellij.plugin.ui.errors

import org.digma.intellij.plugin.ui.common.Laf
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JButton


internal class IconButton(icon: Icon) : JButton(icon) {

    init {
        isOpaque = false
        isContentAreaFilled = false
        isBorderPainted = false
        background = Laf.Colors.TRANSPARENT
        val hoverBackground = Laf.Colors.LIST_ITEM_BACKGROUND

        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                background = hoverBackground
            }

            override fun mouseExited(e: MouseEvent?) {
                background = Laf.Colors.TRANSPARENT
            }

            override fun mousePressed(e: MouseEvent?) {
                background = Laf.Colors.TRANSPARENT
            }

            override fun mouseReleased(e: MouseEvent?) {
                background = hoverBackground
            }
        })
    }

    override fun paintComponent(g: Graphics) {
        g.color = background
        g.fillRect(0, 0, width, height)
        super.paintComponent(g)
    }
}
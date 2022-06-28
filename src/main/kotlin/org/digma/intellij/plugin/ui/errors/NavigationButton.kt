package org.digma.intellij.plugin.ui.errors

import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton

class NavigationButton(val initialText: String, val hoverText: String) : JButton(initialText) {

    init {
        isOpaque = false
        isContentAreaFilled = false
        isBorderPainted = false



        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                text = hoverText
                isOpaque = true
                background = Color.WHITE

            }

            override fun mouseExited(e: MouseEvent?) {
                text = initialText
                isOpaque = false
                isContentAreaFilled = false
            }

            override fun mousePressed(e: MouseEvent?) {
                text = initialText
                isOpaque = false
                isContentAreaFilled = false
            }

            override fun mouseReleased(e: MouseEvent?) {
                text = hoverText
                isOpaque = true
                background = Color.WHITE
            }
        })

    }
}
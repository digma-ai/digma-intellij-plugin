package org.digma.intellij.plugin.ui.errors

import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton

/*
examples:
//    val backButton = NavigationButtonHtml("<html><span style=\"color:#B9B9B9\">${Html.ARROW_LEFT}",
//            "<html><span style=\"color:#000000\">${Html.ARROW_LEFT}")
//    val forwardButton = NavigationButtonHtml("<html><span style=\"color:#B9B9B9\">${Html.ARROW_RIGHT}",
//        "<html><span style=\"color:#000000\">${Html.ARROW_RIGHT}")
 */

internal class NavigationButtonHtml(val initialText: String, val hoverText: String,val myBackground: Color = Color.WHITE) : JButton(initialText) {

    init {
        isOpaque = false
        isContentAreaFilled = false
        isBorderPainted = false
        background = myBackground


        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                text = hoverText
                isOpaque = true
                isContentAreaFilled = true
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
                isContentAreaFilled = true
            }
        })

    }
}
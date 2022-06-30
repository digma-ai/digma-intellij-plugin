package org.digma.intellij.plugin.ui.errors

import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JButton



internal class NavigationButtonIcon(val myIcon: Icon, val myRolloverIcon: Icon,val myBackground: Color = Color.WHITE) : JButton(myIcon) {

    init {
        isOpaque = false
        isContentAreaFilled = false
        isBorderPainted = false
        rolloverIcon = myRolloverIcon
        background = myBackground

        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                isOpaque = true
            }

            override fun mouseExited(e: MouseEvent?) {
                isOpaque = false
            }

            override fun mousePressed(e: MouseEvent?) {
                isOpaque = false
            }

            override fun mouseReleased(e: MouseEvent?) {
                isOpaque = true
            }
        })

    }
}
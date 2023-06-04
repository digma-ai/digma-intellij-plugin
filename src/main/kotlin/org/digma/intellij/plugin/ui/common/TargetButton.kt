package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton

open class TargetButton(project: Project,enabled: Boolean = true) : JButton() {

    private val myIcon = Laf.Icons.General.TARGET
    private val myPressedIcon = Laf.Icons.General.TARGET_PRESSED

    init {
        icon = myIcon
        pressedIcon = myPressedIcon
        isOpaque = true
        isContentAreaFilled = false
        isBorderPainted = false
        border = JBUI.Borders.empty()
        isEnabled = enabled
        background = Laf.Colors.TRANSPARENT
        val hoverBackground = Laf.Colors.LIST_ITEM_BACKGROUND

        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                if (!isEnabled) return
                background = hoverBackground
            }

            override fun mouseExited(e: MouseEvent?) {
                if (!isEnabled) return
                background = Laf.Colors.TRANSPARENT
            }

            override fun mousePressed(e: MouseEvent?) {
                if (!isEnabled) return
                background = Laf.Colors.TRANSPARENT
            }

            override fun mouseReleased(e: MouseEvent?) {
                if (!isEnabled) return
                background = hoverBackground
            }

            override fun mouseClicked(e: MouseEvent?) {
                if (!isEnabled) return
                background = Laf.Colors.TRANSPARENT
            }
        })
    }


    override fun paintComponent(g: Graphics) {
        g.color = background
        g.fillRect(0, 0, width, height)
        super.paintComponent(g)
    }

}
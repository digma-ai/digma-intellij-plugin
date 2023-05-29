package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton

open class TargetButton(project: Project,enabled: Boolean = true) : JButton() {

    private val myIcon = Laf.Icons.General.TARGET
    private val myPressedIcon = Laf.Icons.General.TARGET_PRESSED

    init {
        icon = myIcon
        isOpaque = true
        isContentAreaFilled = false
        isBorderPainted = true
        border = JBUI.Borders.customLine(getBorderColor(), 1)

        isEnabled = enabled
        background = Laf.Colors.TRANSPARENT

        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                if (!isEnabled) return
                isContentAreaFilled = true
                background = Laf.Colors.LIST_ITEM_BACKGROUND
            }

            override fun mouseExited(e: MouseEvent?) {
                if (!isEnabled) return
                isContentAreaFilled = false
                background = Laf.Colors.TRANSPARENT
            }

            override fun mousePressed(e: MouseEvent?) {
                if (!isEnabled) return
                icon = myPressedIcon
                background = Laf.Colors.BUTTON_BACKGROUND
            }

            override fun mouseReleased(e: MouseEvent?) {
                if (!isEnabled) return
                icon = myIcon
                background = Laf.Colors.LIST_ITEM_BACKGROUND
            }

            override fun mouseClicked(e: MouseEvent?) {
                if (!isEnabled) return
//                background = Laf.Colors.TRANSPARENT
            }
        })
    }

    private fun getBorderColor(): Color? {
        return if (JBColor.isBright()) {
            Laf.Colors.LIVE_BUTTON_BORDER_LIGHT
        } else {
            Laf.Colors.LIVE_BUTTON_BORDER_DARK
        }
    }

}
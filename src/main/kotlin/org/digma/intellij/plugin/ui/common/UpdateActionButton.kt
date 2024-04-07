package org.digma.intellij.plugin.ui.common

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton

class UpdateActionButton : JButton() {

    companion object {
        val bg = Laf.Colors.BUTTON_BACKGROUND
    }


    init {
        text = "Update"
        boldFonts(this)
        isContentAreaFilled = false
        horizontalAlignment = CENTER
        background = bg
        isOpaque = true
        border = JBUI.Borders.empty(2)
        margin = JBUI.emptyInsets()

        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                if (!isEnabled) return
                border = JBUI.Borders.customLine(JBColor.GRAY, 2)
            }

            override fun mouseExited(e: MouseEvent?) {
                border = JBUI.Borders.empty(2)
            }

            override fun mousePressed(e: MouseEvent?) {
                if (!isEnabled) return
                background = JBColor.BLUE
            }

            override fun mouseReleased(e: MouseEvent?) {
                if (!isEnabled) return
                background = bg
            }
        })

    }

    override fun setEnabled(b: Boolean) {
        super.setEnabled(b)
        if (b) {
            background = bg
            isBorderPainted = true
        } else {
            background = JBColor.LIGHT_GRAY
            isBorderPainted = false
        }
    }
}
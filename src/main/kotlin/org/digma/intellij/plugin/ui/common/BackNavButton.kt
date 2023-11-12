package org.digma.intellij.plugin.ui.common

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton

class BackNavButton(enabled: Boolean = true) : JButton() {


    init {

        val size = Laf.scaleSize(Laf.Sizes.BUTTON_SIZE_24)
        val buttonsSize = Dimension(size, size)
        preferredSize = buttonsSize
        maximumSize = buttonsSize

        icon = if (JBColor.isBright()) {
            Laf.Icons.Common.NavPrevLight
        } else {
            Laf.Icons.Common.NavPrevDark
        }
        pressedIcon = if (JBColor.isBright()) {
            Laf.Icons.Common.NavPrevLightPressed
        } else {
            Laf.Icons.Common.NavPrevDarkPressed
        }
        isOpaque = false
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
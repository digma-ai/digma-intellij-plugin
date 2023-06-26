package org.digma.intellij.plugin.ui.common

import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

class IconWithLiveIndication(private val icon: Icon) : Icon {
    override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
        icon.paintIcon(c, g, x, y)
        if (g == null)
            return

        g.color = Color(103, 210, 139, (255*0.4).toInt())
        g.fillOval(0, 0, 9, 9)

        g.color = Color.decode("0x67D28B")
        g.fillOval(2, 2, 5, 5)
    }

    override fun getIconWidth(): Int {
        return icon.iconWidth
    }

    override fun getIconHeight(): Int {
        return icon.iconHeight
    }
}
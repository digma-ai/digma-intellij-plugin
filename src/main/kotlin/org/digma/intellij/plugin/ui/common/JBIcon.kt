package org.digma.intellij.plugin.ui.common

import com.intellij.ui.JBColor
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon


class JBIcon constructor(val light: Icon, val dark: Icon) : Icon {

    override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
        getIcon().paintIcon(c,g,x,y)
    }

    override fun getIconWidth(): Int {
        return getIcon().iconWidth
    }

    override fun getIconHeight(): Int {
        return getIcon().iconHeight
    }

    private fun getIcon(): Icon {
        return if (JBColor.isBright())
            light
        else
            dark
    }
}
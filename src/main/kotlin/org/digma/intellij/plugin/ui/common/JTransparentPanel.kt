package org.digma.intellij.plugin.ui.common

import java.awt.Graphics
import javax.swing.JPanel

class JTransparentPanel : JPanel(){
    init {
        isOpaque = false
    }

    override fun paintComponent(g: Graphics) {
        g.color = background
        g.fillRect(0, 0, width, height)
        super.paintComponent(g)
    }
}
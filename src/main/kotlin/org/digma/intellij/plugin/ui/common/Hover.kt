package org.digma.intellij.plugin.ui.common

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JComponent
import javax.swing.JPanel

class Hover(val component: JComponent, val hoverColor: Color) : JPanel() {
    init {
        component.isOpaque = false

        isOpaque = false
        background = component.background
        layout = BorderLayout()
        add(component, BorderLayout.CENTER)

        val mouseListener =object: MouseAdapter(){
            override fun mouseEntered(e: MouseEvent?) {
                background = hoverColor
            }
            override fun mouseExited(e: MouseEvent?) {
                background = component.background
            }
        }
        registerRecursively(this, mouseListener)
    }

    private fun registerRecursively(localComponent: Component, listener: MouseListener){
        localComponent.addMouseListener(listener)
        if (localComponent is Container) {
            localComponent.components.forEach {
                registerRecursively(it, listener)
            }
        }
    }

    override fun paintComponent(g: Graphics) {
        g.color = background
        g.fillRect(0, 0, width, height)
        super.paintComponent(g)
    }
}
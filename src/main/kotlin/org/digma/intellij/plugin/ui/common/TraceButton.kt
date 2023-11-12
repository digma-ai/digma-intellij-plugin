package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.jaegerui.JaegerUIService
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JButton
import javax.swing.SwingConstants

open class TraceButton : JButton("Trace", Laf.Icons.General.TARGET10) {

    init {
        background = Laf.Colors.BUTTON_BACKGROUND
        foreground = Laf.Colors.BUTTON_FONT

        isContentAreaFilled = false
        horizontalAlignment = SwingConstants.CENTER
        isOpaque = true
        border = JBUI.Borders.empty(2)
        margin = JBUI.emptyInsets()

        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            }

            override fun mouseExited(e: MouseEvent?) {
                cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
            }
        })
    }

    fun defineAction(project: Project, traceId: String, title: String) {
        addActionListener {
            project.service<JaegerUIService>().openEmbeddedJaeger(traceId, title)
        }
    }
}
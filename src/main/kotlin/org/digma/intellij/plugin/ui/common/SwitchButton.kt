package org.digma.intellij.plugin.ui.common

import com.intellij.ui.JBColor
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import javax.swing.Timer


class SwitchButton(width: Int, height: Int, var selected: Boolean) : Component() {


    fun isSelectedState(): Boolean {
        return selected
    }

    fun setSelectedState(selected: Boolean) {
        this.selected = selected
        timer.start()
        runEvent()
    }

    private lateinit var timer: Timer
    private var location = 0f
    private var mouseOver = false
    private val speed = 0.1f
    private var events: MutableList<EventSwitchSelected> = mutableListOf()

    init {
        isOpaque
        background = JBColor(JBColor.CYAN,Color(0, 174, 255))
        preferredSize = Dimension(width, height)
        foreground = JBColor.WHITE
        cursor = Cursor(Cursor.HAND_CURSOR)
        location = 2f
        timer = Timer(0) {
            if (isSelectedState()) {
                val endLocation = width - height + 2
                if (location < endLocation) {
                    location += speed
                    repaint()
                } else {
                    timer.stop()
                    location = endLocation.toFloat()
                    repaint()
                }
            } else {
                val endLocation = 2
                if (location > endLocation) {
                    location -= speed
                    repaint()
                } else {
                    timer.stop()
                    location = endLocation.toFloat()
                    repaint()
                }
            }
        }
        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(me: MouseEvent?) {
                mouseOver = true
            }

            override fun mouseExited(me: MouseEvent?) {
                mouseOver = false
            }

            override fun mouseReleased(me: MouseEvent?) {
                if (SwingUtilities.isLeftMouseButton(me) && mouseOver) {
                    selected = !selected
                    timer.start()
                    runEvent()
                }
            }
        })

        timer.start()
    }


    override fun paint(grphcs: Graphics) {
        val g2 = grphcs as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val width = width
        val height = height
        val alpha: Float = getAlpha()
        if (alpha < 1) {
            g2.color = JBColor.GRAY
            g2.fillRoundRect(0, 0, width, height, height, height)
        }
        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
        g2.color = background
        g2.fillRoundRect(0, 0, width, height, height, height)
        g2.color = foreground
        g2.composite = AlphaComposite.SrcOver
        g2.fillOval(location.toInt(), 2, height - 4, height - 4)
        super.paint(grphcs)
    }


    private fun getAlpha(): Float {
        val width = (width - height).toFloat()
        var alpha: Float = (location - 2) / width
        if (alpha.isNaN()){
            return 0.0f
        }
        if (alpha < 0) {
            alpha = 0.0f
        }
        if (alpha > 1) {
            alpha = 1.0f
        }
        return alpha
    }


    private fun runEvent() {
        for (event in events) {
            event.onSelected(selected)
        }
    }

    fun addEventSelected(event: EventSwitchSelected) {
        events.add(event)
    }


    interface EventSwitchSelected {
        fun onSelected(selected: Boolean)
    }
}
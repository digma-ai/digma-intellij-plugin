package org.digma.intellij.plugin.ui

import com.intellij.util.ui.JBUI
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class PieChart : JComponent() {

    val items: ArrayList<Item> = ArrayList()
    var marginArcSize: Int = 4
    var minArcSize: Int = 4

    init {
        isOpaque = false
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val border = super.getBorder()?.getBorderInsets(this) ?: JBUI.emptyInsets()
        val graphics = g as Graphics2D
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        var x = border.left
        var y = border.top
        var d = min(width - border.left - border.right, height - border.top - border.bottom)

        // gray border 1px
        graphics.color = Color(0x676673)
        graphics.fillOval(x, y, d, d)
        x += 1
        y += 1
        d -= 2

        // inner dark bg
        graphics.color = Color(0x3f4247)
        graphics.fillOval(x, y, d, d)
        x += 3
        y += 3
        d -= 6

        // arcs
        val arcs = calcArcs(d)
        graphics.stroke = BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        for (arc in arcs) {
            graphics.color = arc.color
            graphics.drawArc(x, y, d, d, arc.startAngle, arc.angle)
        }
    }

    private fun calcArcs(diameter: Int): List<Arc> {
        val sortedItems = items.sortedBy { i -> i.weight }

        val arcs = ArrayList<Arc>()

        val minArcAngle = ceil(Math.toDegrees(asin(minArcSize * 2.0 / diameter))).toInt()
        val marginArcAngle = ceil(Math.toDegrees(asin(marginArcSize * 2.0 / diameter))).toInt()
        val wholeWeight = items.sumOf { i -> i.weight }
        val wholeAngle = 360 - marginArcAngle * items.size
        var angle = 0

        for (i in sortedItems.indices) {
            if (i < sortedItems.size - 1) {
                val fraction = sortedItems[i].weight / wholeWeight
                val arcAngle = max((wholeAngle * fraction).toInt(), minArcAngle)
                arcs.add(Arc(sortedItems[i].color, angle, arcAngle))
                angle += arcAngle + marginArcAngle
            } else { // last
                arcs.add(Arc(sortedItems[i].color, angle, 360 - marginArcAngle - angle))
            }
        }

        return arcs
    }

    private class Arc(val color: Color, val startAngle: Int, val angle: Int)

    class Item(val color: Color, val weight: Double)
}
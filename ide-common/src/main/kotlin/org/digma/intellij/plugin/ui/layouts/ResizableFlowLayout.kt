package org.digma.intellij.plugin.ui.layouts

import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.FlowLayout
import kotlin.math.max

private typealias SizeGetter = (Component) -> Dimension

// By default, the FlowLayout layout manager does not resize its container when
// a new row is created, but we can achieve this with a custom implementation.
//
// (This implementation is based on a ChatGPT solution)

class ResizableFlowLayout : FlowLayout {
    constructor() : super()
    constructor(align: Int) : super(align)
    constructor(align: Int, hgap: Int, vgap: Int) : super(align, hgap, vgap)

    override fun preferredLayoutSize(target: Container): Dimension {
        return calcRequiredSize(target) { c -> c.preferredSize }
    }

    override fun minimumLayoutSize(target: Container): Dimension {
        return calcRequiredSize(target) { c -> c.minimumSize }
    }

    private fun calcRequiredSize(target: Container, getSize: SizeGetter): Dimension {
        synchronized(target.treeLock) {
            var targetWidth: Int = target.width
            if (targetWidth == 0) {
                targetWidth = Int.MAX_VALUE
            }
            val maxWidth = targetWidth - (target.insets.left + target.insets.right + hgap * 2)
            var rowWidth = 0
            var rowHeight = 0
            var totalHeight = 0
            for (c in target.components){
                if (!c.isVisible)
                    continue

                val componentSize: Dimension = getSize(c)
                if (rowWidth + componentSize.width > maxWidth) {
                    totalHeight += rowHeight + vgap
                    rowWidth = 0
                    rowHeight = 0
                }
                rowWidth += componentSize.width + hgap
                rowHeight = max(rowHeight, componentSize.height)
            }
            totalHeight += rowHeight
            return Dimension(targetWidth, totalHeight + target.insets.top + target.insets.bottom + vgap * 2)
        }
    }

    // In case the default implementation of FlowLayout.layoutContainer()
    // won't be good enough, uncomment this function, and fine tune it

//    override fun layoutContainer(target: Container) {
//        synchronized(target.treeLock) {
//            val maxWidth= target.width - (target.insets.left + target.insets.right + hgap * 2)
//            var x = target.insets.left + hgap
//            var y = target.insets.top + vgap
//            var rowHeight = 0
//            for (c in target.components) {
//                if (!c.isVisible)
//                    continue
//
//                val componentPreferredSize = c.preferredSize
//                if (x + componentPreferredSize.width > maxWidth) {
//                    x = target.insets.left + hgap
//                    y += rowHeight + vgap
//                    rowHeight = 0
//                }
//                c.setBounds(x, y, componentPreferredSize.width, componentPreferredSize.height)
//                x += componentPreferredSize.width + hgap
//                rowHeight = max(rowHeight, componentPreferredSize.height)
//            }
//        }
//    }
}
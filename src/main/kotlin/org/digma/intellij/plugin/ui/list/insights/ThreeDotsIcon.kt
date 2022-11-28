package org.digma.intellij.plugin.ui.list.insights

import org.digma.intellij.plugin.ui.common.ColorGetter
import org.digma.intellij.plugin.ui.common.SvgIcon

class ThreeDotsIcon(path: String, getColor: ColorGetter? = null) : SvgIcon(path, getColor) {
    companion object {
        fun asIs(path: String): ThreeDotsIcon{
            return ThreeDotsIcon(path)
        }
    }
}
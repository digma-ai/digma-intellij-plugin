package org.digma.intellij.plugin.ui.common

import com.intellij.ui.GuiUtils
import com.intellij.ui.JBColor
import java.awt.Color


object Html {
    const val ARROW_RIGHT: String = "&#8594;"
    const val ARROW_LEFT: String = "&#8592;"
}

fun JBColor.getHex() : String{
    val c = Color(this.rgb)
    return c.getHex()
}

fun Color.getHex() : String{
    val s = GuiUtils.colorToHex(this)
    return if (s.startsWith("#")) s else "#$s"
}

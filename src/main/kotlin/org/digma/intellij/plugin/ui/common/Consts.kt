package org.digma.intellij.plugin.ui.common

import com.intellij.ui.GuiUtils
import com.intellij.ui.JBColor
import java.awt.Color



object DigmaColors{
    //val LIST_ITEM_BACKGROUND: JBColor = JBColor(0xE6EEF7, 0x45494A)
    val LIST_ITEM_BACKGROUND: JBColor = JBColor(Color(0, 0, 50, 15), Color(200, 200, 255, 15))
    val TRANSPARENT: Color = Color(0, 0, 0, 0)
    val BLUE_LIGHT_SHADE: Color = Color(0x8f90ff)
    val ERROR_RED: Color = Color(0xf95959)      // same as in VS Code plugin
    val ERROR_ORANGE: Color = Color(0xfdb44b)   // same as in VS Code plugin
    val ERROR_GREEN: Color = Color(0x7dd87d)    // same as in VS Code plugin
    val SIMPLE_ICON_COLOR: JBColor = JBColor(0x222222, 0xDDDDDD)
    val GRAY: Color = Color(0x8A8A8A)
}

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

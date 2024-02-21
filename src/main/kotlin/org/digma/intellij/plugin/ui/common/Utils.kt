package org.digma.intellij.plugin.ui.common

import com.intellij.ui.GuiUtils
import com.intellij.ui.JBColor
import java.awt.Color


fun JBColor.getHex(): String = Color(this.rgb).getHex()

fun Color.getHex(): String {
    val s = GuiUtils.colorToHex(this)
    return if (s.startsWith("#")) s else "#$s"
}


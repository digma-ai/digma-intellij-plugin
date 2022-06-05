package org.digma.intellij.plugin.ui.list

import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.Color


fun panelListBackground(): Color {
    var default = Color.DARK_GRAY
    if (UIUtil.isUnderDarcula()) {
        default = Color(38, 38, 38)
    }
    return JBColor.namedColor("Editor.background", default)
}
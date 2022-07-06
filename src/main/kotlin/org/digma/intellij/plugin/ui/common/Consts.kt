package org.digma.intellij.plugin.ui.common

import java.awt.Color


object Html {
    const val ARROW_RIGHT: String = "&#8594;"
    const val ARROW_LEFT: String = "&#8592;"

    const val RED: String = "#f95959"

    const val DARK_GRAY2: String = "#8A8A8A"
    const val LIGHT_WHITE: String = "#FFFFF7"
    const val CHINESE_SILVER: String = "#CCCCCC"
    const val BLUE_LIGHT_SHADE: String = "#8f90ff"

}

object Swing {
    val ERROR_RED = Color(249, 89, 89)     // " #f95959 - same as in VS Code plugin
    val ERROR_ORANGE = Color(253, 180, 75) // " #fdb44b - same as in VS Code plugin
    val ERROR_GREEN = Color(125, 216, 125) // " #7dd87d - same as in VS Code plugin

    val BLUE_LIGHT_SHADE: Color = Color.decode(Html.BLUE_LIGHT_SHADE)

    val NO_INFO_PANEL_BACKGROUND:Color = Color(54, 1, 1,50)
}




package org.digma.intellij.plugin.ui.common

import java.awt.Color


object Html {
    const val ARROW_RIGHT: String = "&#8594;"

    const val RED: String = "#f14c4c"

    const val STANDARD_LINK: String = "#0000EE"
    const val DARK_GRAY: String = "#A8A8A8"
    const val DARK_GRAY2: String = "#ACACAC"
    const val BRIGHT_WHITE: String = "#FFFFFF"
    const val LIGHT_WHITE: String = "#FFFFF7"
    const val WHITE_SMOKE: String = "#F5F5F5"
    const val WHITE_GOHST: String = "#F8F8FF"
    const val WHITE_PEARL: String = "#F8F6F0"
    const val CHINESE_SILVER: String = "#CCCCCC"


    const val INSIGHTS_TITLE: String = CHINESE_SILVER
    const val INSIGHTS_WHITE: String = CHINESE_SILVER
    const val INSIGHTS_SMOKED: String = DARK_GRAY2


    fun span(color: String):String{
        return "<span style=\"color:$color\">"
    }

}

object Swing {
    val ERROR_RED = Color(249, 89, 89)     // " #f95959 - same as in VS Code plugin
    val ERROR_ORANGE = Color(253, 180, 75) // " #fdb44b - same as in VS Code plugin
    val ERROR_GREEN = Color(125, 216, 125) // " #7dd87d - same as in VS Code plugin
}
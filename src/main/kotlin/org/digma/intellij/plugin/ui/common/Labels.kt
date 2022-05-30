package org.digma.intellij.plugin.ui.common

import java.awt.Color

fun asHtml(content: String): String {
    return "<html><body>${content}</body>"
}

fun wrapCentered(content: String): String {
    return "<center>${content}</center>"
}

object HtmlConsts {
    const val RED_COLOR: String = "#f14c4c" // same as in VS Code plugin

    const val ARROW_RIGHT: String = "&#8594;"
}

object Swing {
    val ERROR_RED = Color(249, 89, 89)     // " #f95959 - same as in VS Code plugin
    val ERROR_ORANGE = Color(253, 180, 75) // " #fdb44b - same as in VS Code plugin
    val ERROR_GREEN = Color(125, 216, 125) // " #7dd87d - same as in VS Code plugin
}
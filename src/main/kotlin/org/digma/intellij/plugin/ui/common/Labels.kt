package org.digma.intellij.plugin.ui.common

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

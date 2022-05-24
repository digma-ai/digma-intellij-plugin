package org.digma.intellij.plugin.ui.common

fun asHtml(content: String): String {
    return "<html><body>${content}</body>"
}

fun wrapCentered(content: String): String {
    return "<center>${content}</center>"
}

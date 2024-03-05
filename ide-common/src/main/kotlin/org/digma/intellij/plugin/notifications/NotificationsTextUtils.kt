package org.digma.intellij.plugin.notifications


fun asHtml(content: String?): String {
    return "<html><body>${content.orEmpty()}</body>"
}
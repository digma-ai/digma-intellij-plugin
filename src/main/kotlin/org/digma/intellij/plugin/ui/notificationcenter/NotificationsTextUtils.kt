package org.digma.intellij.plugin.ui.notificationcenter


fun asHtml(content: String?): String {
    return "<html><body>${content.orEmpty()}</body>"
}
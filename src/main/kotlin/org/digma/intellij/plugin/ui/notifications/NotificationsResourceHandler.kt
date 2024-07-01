package org.digma.intellij.plugin.ui.notifications

import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.jcef.BaseResourceHandler
import org.digma.intellij.plugin.ui.jcef.getProject
import java.io.InputStream

class NotificationsResourceHandler(browser: CefBrowser, private val notificationViewMode: NotificationViewMode, path: String) :
    BaseResourceHandler(path,browser) {


    override fun isIndexHtml(path: String): Boolean {
        return path.endsWith("index.html", true)
    }

    override fun buildIndexFromTemplate(path: String): InputStream? {
        val project = getProject(browser)
        if (project == null) {
            Log.log(logger::warn, "project is null , should never happen")
            ErrorReporter.getInstance().reportError(null, "NotificationsResourceHandler.buildIndexFromTemplate", "project is null", mapOf())
            return null
        }
        return NotificationsIndexTemplateBuilder(notificationViewMode).build(project)
    }
}
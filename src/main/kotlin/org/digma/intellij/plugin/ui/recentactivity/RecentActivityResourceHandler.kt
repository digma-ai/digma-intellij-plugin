package org.digma.intellij.plugin.ui.recentactivity

import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.jcef.BaseResourceHandler
import org.digma.intellij.plugin.ui.jcef.getProject
import java.io.InputStream

class RecentActivityResourceHandler(browser: CefBrowser, path: String) :
    BaseResourceHandler(path, browser) {


    override fun isIndexHtml(path: String): Boolean {
        return path.endsWith("index.html", true)
    }

    override fun buildIndexFromTemplate(path: String): InputStream? {
        val project = getProject(browser)
        if (project == null) {
            Log.log(logger::warn, "project is null , should never happen")
            ErrorReporter.getInstance().reportError(null, "RecentActivityResourceHandler.buildIndexFromTemplate", "project is null", mapOf())
            return null
        }
        return RecentActivityIndexTemplateBuilder().build(project)
    }
}
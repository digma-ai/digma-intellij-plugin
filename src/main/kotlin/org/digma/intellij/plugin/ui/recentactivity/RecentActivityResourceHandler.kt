package org.digma.intellij.plugin.ui.recentactivity

import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.jcef.BaseResourceHandler
import org.digma.intellij.plugin.ui.jcef.getProject
import java.io.InputStream

class RecentActivityResourceHandler(browser: CefBrowser, path: String) :
    BaseResourceHandler(path, browser) {

    override fun getResourceFolderName(): String {
        return RECENT_ACTIVITY_RESOURCE_FOLDER_NAME
    }

    override fun buildEnvJsFromTemplate(path: String): InputStream? {
        val project = getProject(browser)
        if (project == null) {
            Log.log(logger::warn, "project is null , should never happen")
            ErrorReporter.getInstance().reportError(null, "RecentActivityResourceHandler.buildEnvJsFromTemplate", "project is null", mapOf())
            return null
        }
        return RecentActivityEnvJsTemplateBuilder(path).build(project)
    }
}
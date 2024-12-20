package org.digma.intellij.plugin.ui.troubleshooting

import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.jcef.BaseResourceHandler
import org.digma.intellij.plugin.ui.jcef.getProject
import java.io.InputStream

class TroubleshootingResourceHandler(browser: CefBrowser, path: String) : BaseResourceHandler(path, browser) {

    override fun getResourceFolderName(): String {
        return TROUBLESHOOTING_RESOURCE_FOLDER_NAME
    }

    override fun buildEnvJsFromTemplate(path: String): InputStream? {
        val project = getProject(browser)
        if (project == null) {
            Log.log(logger::warn, "project is null , should never happen")
            ErrorReporter.getInstance().reportError(null, "TroubleshootingResourceHandler.buildEnvJsFromTemplate", "project is null", mapOf())
            return null
        }
        return TroubleshootingEnvJsTemplateBuilder(path).build(project)
    }
}

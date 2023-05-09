package org.digma.intellij.plugin.toolwindow.common

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.CefApp
import org.digma.intellij.plugin.common.JBCefBrowserBuilderCreator

class CustomViewerWindow(
    val project: Project,
    private val resourceFolderName: String,
    private val indexTemplateData: Map<String, Any>?
) {
    private val browserDisposer = Disposer.newDisposable()
    private val webView: JBCefBrowser = getBrowser()

    private fun getBrowser(): JBCefBrowser {
        val jbCefBrowser = JBCefBrowserBuilderCreator.create().build()
        registerAppSchemeHandler()
        jbCefBrowser.loadURL("https://$resourceFolderName/index.html")
        Disposer.register(browserDisposer, jbCefBrowser)
        return jbCefBrowser
    }

    fun getWebView(): JBCefBrowser = webView

    private fun registerAppSchemeHandler(): Boolean {
        return CefApp.getInstance()
            .registerSchemeHandlerFactory(
                "https",
                resourceFolderName,
                CustomSchemeHandlerFactory(resourceFolderName, indexTemplateData)
            )
    }

    fun dispose() {
        Disposer.dispose(browserDisposer)
    }
}

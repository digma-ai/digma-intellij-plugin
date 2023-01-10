package org.digma.intellij.plugin.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBuilder
import org.cef.CefApp

class CustomViewerWindow(project: Project) {
    private val webView: JBCefBrowser = getBrowser(project)
    private fun getBrowser(project: Project): JBCefBrowser {
        val jbCefBrowserBuilder = JBCefBrowserBuilder()
        jbCefBrowserBuilder.setOffScreenRendering(false)
        // this provides us an opportunity to open DevTools
        jbCefBrowserBuilder.setEnableOpenDevToolsMenuItem(true)
        val jbCefBrowser = jbCefBrowserBuilder.build()

        registerAppSchemeHandler()
        jbCefBrowser.loadURL("http://myapp/index.html")
        Disposer.register(project, jbCefBrowser)
        return jbCefBrowser
    }

    fun getWebView(): JBCefBrowser = webView

    private fun registerAppSchemeHandler(): Boolean {
        return CefApp
                .getInstance()
                .registerSchemeHandlerFactory(
                        "http",
                        "myapp",
                        CustomSchemeHandlerFactory()
                )
    }
}

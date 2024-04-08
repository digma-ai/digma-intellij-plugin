package org.digma.intellij.plugin.ui.jcef

import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefCallback
import org.cef.callback.CefSchemeHandlerFactory
import org.cef.handler.CefResourceHandler
import org.cef.handler.CefResourceHandlerAdapter
import org.cef.network.CefRequest
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import java.awt.Desktop
import java.net.URI

class MailtoSchemaHandlerFactory : CefSchemeHandlerFactory {

    override fun create(browser: CefBrowser?, frame: CefFrame?, schemeName: String?, request: CefRequest?): CefResourceHandler {
        return MailtoResourceHandler()
    }

    fun getSchema(): String {
        return "mailto"
    }
}


class MailtoResourceHandler : CefResourceHandlerAdapter() {

    override fun processRequest(request: CefRequest?, callback: CefCallback?): Boolean {
        try {
            if (request != null) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().mail(URI(request.url))
                }
            }
        } catch (e: Exception) {
            ErrorReporter.getInstance().reportError("MailtoResourceHandler.processRequest", e)
        }
        callback?.Continue()
        return true
    }
}
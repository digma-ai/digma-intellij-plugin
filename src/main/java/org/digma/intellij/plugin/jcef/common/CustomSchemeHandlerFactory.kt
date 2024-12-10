package org.digma.intellij.plugin.jcef.common

import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefSchemeHandlerFactory
import org.cef.handler.CefResourceHandler
import org.cef.network.CefRequest

class CustomSchemeHandlerFactory(
    private var domainNameName: String,
    private var templateFolderName: String,
    private var resourceFolderName: String,
    private var indexTemplateData: Map<String, Any>?
) : CefSchemeHandlerFactory {

    override fun create(
        browser: CefBrowser?,
        frame: CefFrame?,
        schemeName: String?,
        request: CefRequest?
    ): CefResourceHandler {
        return CustomResourceHandler(domainNameName,templateFolderName,resourceFolderName, indexTemplateData)
    }
}
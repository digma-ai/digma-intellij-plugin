package org.digma.intellij.plugin.ui.mainapp

import org.cef.browser.CefBrowser
import org.cef.handler.CefResourceHandler
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory

class MainAppSchemeHandlerFactory : BaseSchemeHandlerFactory() {

    override fun createResourceHandler(browser: CefBrowser, resourcePath: String): CefResourceHandler {
        return MainAppResourceHandler(browser, resourcePath)
    }

    override fun getSchema(): String {
        return MAIN_APP_SCHEMA
    }

    override fun getDomain(): String {
        return MAIN_APP_DOMAIN_NAME
    }
}
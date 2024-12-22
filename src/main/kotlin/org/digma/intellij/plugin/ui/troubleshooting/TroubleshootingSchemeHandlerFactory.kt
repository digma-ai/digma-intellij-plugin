package org.digma.intellij.plugin.ui.troubleshooting

import org.cef.browser.CefBrowser
import org.cef.handler.CefResourceHandler
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory

class TroubleshootingSchemeHandlerFactory : BaseSchemeHandlerFactory() {

    override fun createResourceHandler(browser: CefBrowser, resourcePath: String): CefResourceHandler {
        return TroubleshootingResourceHandler(browser, resourcePath)
    }

    override fun getSchema(): String {
        return TROUBLESHOOTING_SCHEMA
    }

    override fun getDomain(): String {
        return TROUBLESHOOTING_DOMAIN_NAME
    }
}
package org.digma.intellij.plugin.ui.recentactivity

import org.cef.browser.CefBrowser
import org.cef.handler.CefResourceHandler
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory

class RecentActivitySchemeHandlerFactory : BaseSchemeHandlerFactory() {

    override fun createResourceHandler(browser: CefBrowser, resourcePath: String): CefResourceHandler {
        return RecentActivityResourceHandler(browser, resourcePath)
    }

    override fun getSchema(): String {
        return RECENT_ACTIVITY_APP_SCHEMA
    }

    override fun getDomain(): String {
        return RECENT_ACTIVITY_DOMAIN_NAME
    }
}
package org.digma.intellij.plugin.ui.recentactivity

import org.cef.browser.CefBrowser
import org.cef.handler.CefResourceHandler
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory

class RecentActivitySchemeHandlerFactory : BaseSchemeHandlerFactory() {


    override fun createResourceHandler(resourceName: String, resourceExists: Boolean, browser: CefBrowser): CefResourceHandler {
        return if (resourceExists) {
            RecentActivityResourceHandler(browser, resourceName)
        } else {
            RecentActivityResourceHandler(browser, "$RECENT_ACTIVITY_RESOURCE_FOLDER_NAME/index.html")
        }
    }

    override fun getSchema(): String {
        return RECENT_ACTIVITY_APP_SCHEMA
    }

    override fun getDomain(): String {
        return RECENT_ACTIVITY_DOMAIN_NAME
    }

    override fun getResourceFolderName(): String {
        return RECENT_ACTIVITY_RESOURCE_FOLDER_NAME
    }
}
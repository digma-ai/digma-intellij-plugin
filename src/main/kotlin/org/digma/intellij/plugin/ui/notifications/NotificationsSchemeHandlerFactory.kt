package org.digma.intellij.plugin.ui.notifications

import org.cef.browser.CefBrowser
import org.cef.handler.CefResourceHandler
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory

class NotificationsSchemeHandlerFactory(val notificationViewMode: NotificationViewMode) : BaseSchemeHandlerFactory() {


    override fun createResourceHandler(resourceName: String, resourceExists: Boolean, browser: CefBrowser): CefResourceHandler {

        return if (resourceExists) {
            NotificationsResourceHandler(browser, notificationViewMode, resourceName)
        } else {
            NotificationsResourceHandler(browser, notificationViewMode, "$NOTIFICATIONS_RESOURCE_FOLDER_NAME/index.html")
        }
    }

    override fun getSchema(): String {
        return NOTIFICATIONS_APP_SCHEMA
    }

    override fun getDomain(): String {
        return NOTIFICATIONS_DOMAIN_NAME
    }

    override fun getResourceFolderName(): String {
        return NOTIFICATIONS_RESOURCE_FOLDER_NAME
    }
}
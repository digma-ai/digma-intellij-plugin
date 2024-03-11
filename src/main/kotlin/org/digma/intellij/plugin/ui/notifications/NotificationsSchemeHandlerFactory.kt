package org.digma.intellij.plugin.ui.notifications

import com.intellij.openapi.project.Project
import org.cef.handler.CefResourceHandler
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory

class NotificationsSchemeHandlerFactory(project: Project, val notificationViewMode: NotificationViewMode) : BaseSchemeHandlerFactory(project) {


    override fun createResourceHandler(resourceName: String, resourceExists: Boolean): CefResourceHandler {

        return if (resourceExists) {
            NotificationsResourceHandler(project, notificationViewMode, resourceName)
        } else {
            NotificationsResourceHandler(project, notificationViewMode, "$NOTIFICATIONS_RESOURCE_FOLDER_NAME/index.html")
        }
    }

    override fun getSchema(): String {
        return "http"
    }

    override fun getDomain(): String {
        return NOTIFICATIONS_DOMAIN_NAME
    }

    override fun getResourceFolderName(): String {
        return NOTIFICATIONS_RESOURCE_FOLDER_NAME
    }
}
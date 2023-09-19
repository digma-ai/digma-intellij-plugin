package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.openapi.project.Project
import org.cef.handler.CefResourceHandler
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory

class RecentActivitySchemeHandlerFactory(val project: Project) : BaseSchemeHandlerFactory() {
    override fun createResourceHandler(resourceName: String, resourceExists: Boolean): CefResourceHandler {
        return if (resourceExists) {
            RecentActivityResourceHandler(project, resourceName)
        } else {
            RecentActivityResourceHandler(project, "$RECENT_ACTIVITY_RESOURCE_FOLDER_NAME/index.html")
        }
    }

    override fun getSchema(): String {
        return "https"
    }

    override fun getDomain(): String {
        return RECENT_ACTIVITY_DOMAIN_NAME
    }

    override fun getResourceFolderName(): String {
        return RECENT_ACTIVITY_RESOURCE_FOLDER_NAME
    }
}
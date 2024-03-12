package org.digma.intellij.plugin.ui.navigation

import com.intellij.openapi.project.Project
import org.cef.handler.CefResourceHandler
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory

class NavigationSchemeHandlerFactory(project: Project) : BaseSchemeHandlerFactory(project) {

    override fun createResourceHandler(resourceName: String, resourceExists: Boolean): CefResourceHandler {
        return if (resourceExists) {
            NavigationResourceHandler(project, resourceName)
        } else {
            NavigationResourceHandler(project, "$NAVIGATION_APP_RESOURCE_FOLDER_NAME/index.html")
        }
    }

    override fun getSchema(): String {
        return "https"
    }

    override fun getDomain(): String {
        return NAVIGATION_APP_DOMAIN_NAME
    }

    override fun getResourceFolderName(): String {
        return NAVIGATION_APP_RESOURCE_FOLDER_NAME
    }
}
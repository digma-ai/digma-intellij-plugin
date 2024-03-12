package org.digma.intellij.plugin.ui.assets

import com.intellij.openapi.project.Project
import org.cef.handler.CefResourceHandler
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory

class AssetsSchemeHandlerFactory(project: Project) : BaseSchemeHandlerFactory(project) {

    override fun createResourceHandler(resourceName: String, resourceExists: Boolean): CefResourceHandler {
        return if (resourceExists) {
            AssetsResourceHandler(project, resourceName)
        } else {
            AssetsResourceHandler(project, "$ASSETS_APP_RESOURCE_FOLDER_NAME/index.html")
        }
    }

    override fun getSchema(): String {
        return "https"
    }

    override fun getDomain(): String {
        return ASSETS_APP_DOMAIN_NAME
    }

    override fun getResourceFolderName(): String {
        return ASSETS_APP_RESOURCE_FOLDER_NAME
    }
}
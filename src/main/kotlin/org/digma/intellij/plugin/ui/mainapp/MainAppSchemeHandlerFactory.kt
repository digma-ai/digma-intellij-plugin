package org.digma.intellij.plugin.ui.mainapp

import com.intellij.openapi.project.Project
import org.cef.handler.CefResourceHandler
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory

class MainAppSchemeHandlerFactory(project: Project) : BaseSchemeHandlerFactory(project) {

    override fun createResourceHandler(resourceName: String, resourceExists: Boolean): CefResourceHandler {
        return if (resourceExists) {
            MainAppResourceHandler(project, resourceName)
        } else {
            MainAppResourceHandler(project, "$MAIN_APP_RESOURCE_FOLDER_NAME/index.html")
        }
    }

    override fun getSchema(): String {
        return MAIN_APP_SCHEMA
    }

    override fun getDomain(): String {
        return MAIN_APP_DOMAIN_NAME
    }

    override fun getResourceFolderName(): String {
        return MAIN_APP_RESOURCE_FOLDER_NAME
    }
}
package org.digma.intellij.plugin.ui.insights

import com.intellij.openapi.project.Project
import org.cef.handler.CefResourceHandler
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory

class InsightsSchemeHandlerFactory(project: Project) : BaseSchemeHandlerFactory(project) {

    override fun createResourceHandler(resourceName: String, resourceExists: Boolean): CefResourceHandler {
        return if (resourceExists) {
            InsightsResourceHandler(project, resourceName)
        } else {
            InsightsResourceHandler(project, "$INSIGHTS_APP_RESOURCE_FOLDER_NAME/index.html")
        }
    }

    override fun getSchema(): String {
        return "https"
    }

    override fun getDomain(): String {
        return INSIGHTS_APP_DOMAIN_NAME
    }

    override fun getResourceFolderName(): String {
        return INSIGHTS_APP_RESOURCE_FOLDER_NAME
    }
}
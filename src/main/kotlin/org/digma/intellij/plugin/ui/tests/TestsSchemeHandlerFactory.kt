package org.digma.intellij.plugin.ui.tests

import com.intellij.openapi.project.Project
import org.cef.handler.CefResourceHandler
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory

class TestsSchemeHandlerFactory(val project: Project) : BaseSchemeHandlerFactory() {
    override fun createResourceHandler(resourceName: String, resourceExists: Boolean): CefResourceHandler {
        return if (resourceExists) {
            TestsResourceHandler(project, resourceName)
        } else {
            TestsResourceHandler(project, "$TEST_APP_RESOURCE_FOLDER_NAME/index.html")
        }
    }

    override fun getSchema(): String {
        return "https"
    }

    override fun getDomain(): String {
        return TESTS_APP_DOMAIN_NAME
    }

    override fun getResourceFolderName(): String {
        return TEST_APP_RESOURCE_FOLDER_NAME
    }
}
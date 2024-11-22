package org.digma.intellij.plugin.ui.mainapp

import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.cef.handler.CefResourceHandler
import org.digma.intellij.plugin.ui.jcef.ApiProxyResourceHandler
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory
import java.net.URL

class MainAppSchemeHandlerFactory : BaseSchemeHandlerFactory() {
    override fun createProxyHandler(project: Project, url: URL): CefResourceHandler? {
        if (ApiProxyResourceHandler.isApiProxyCall(url)) {
            return ApiProxyResourceHandler(project)
        }
        return null
    }

    override fun createResourceHandler(resourceName: String, resourceExists: Boolean, browser: CefBrowser): CefResourceHandler {
        return if (resourceExists) {
            MainAppResourceHandler(browser, resourceName)
        } else {
            MainAppResourceHandler(browser, "$MAIN_APP_RESOURCE_FOLDER_NAME/index.html")
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
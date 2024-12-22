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

    override fun createResourceHandler(browser: CefBrowser, resourcePath: String): CefResourceHandler {
        return MainAppResourceHandler(browser, resourcePath)
    }

    override fun getSchema(): String {
        return MAIN_APP_SCHEMA
    }

    override fun getDomain(): String {
        return MAIN_APP_DOMAIN_NAME
    }
}
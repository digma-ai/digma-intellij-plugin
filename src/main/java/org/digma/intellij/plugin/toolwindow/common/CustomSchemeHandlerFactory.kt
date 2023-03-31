package org.digma.intellij.plugin.toolwindow.common

import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefSchemeHandlerFactory
import org.cef.handler.CefResourceHandler
import org.cef.network.CefRequest

class CustomSchemeHandlerFactory(private var project: Project, private var resourceFolderName: String) : CefSchemeHandlerFactory {

    override fun create(browser: CefBrowser?, frame: CefFrame?, schemeName: String?, request: CefRequest?): CefResourceHandler {
        return CustomResourceHandler(project, resourceFolderName)
    }
}
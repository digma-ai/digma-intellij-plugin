package org.digma.intellij.plugin.ui.jcef

import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefSchemeHandlerFactory
import org.cef.handler.CefResourceHandler
import org.cef.network.CefRequest
import java.net.MalformedURLException
import java.net.URL

abstract class BaseSchemeHandlerFactory : CefSchemeHandlerFactory {


    override fun create(browser: CefBrowser?, frame: CefFrame?, schemeName: String, request: CefRequest): CefResourceHandler? {

        val url = getUrl(request)

        if (url != null) {

            val host = url.host
            val file = url.file

            if (getDomain() == host && getSchema() == schemeName) {
                val resourceName = getResourceFolderName() + file
                val resource = javaClass.getResource(resourceName)
                return createResourceHandler(resourceName, resource != null)
            }
        }
        return null
    }


    private fun getUrl(request: CefRequest): URL? {
        return try {
            URL(request.url)
        } catch (e: MalformedURLException) {
            null
        }
    }


    abstract fun createResourceHandler(resourceName: String, resourceExists: Boolean): CefResourceHandler
    abstract fun getSchema(): String
    abstract fun getDomain(): String
    abstract fun getResourceFolderName(): String

}
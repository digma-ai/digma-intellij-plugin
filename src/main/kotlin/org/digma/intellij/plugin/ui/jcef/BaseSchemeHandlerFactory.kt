package org.digma.intellij.plugin.ui.jcef

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefSchemeHandlerFactory
import org.cef.handler.CefResourceHandler
import org.cef.network.CefRequest
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import java.net.MalformedURLException
import java.net.URI
import java.net.URL


abstract class BaseSchemeHandlerFactory : CefSchemeHandlerFactory {

    private val logger: Logger = Logger.getInstance(this::class.java)

    override fun create(browser: CefBrowser?, frame: CefFrame?, schemeName: String, request: CefRequest): CefResourceHandler? {

        //browser and project should never be null.
        if (browser == null) {
            Log.log(logger::warn, "browser is null , should never happen")
            ErrorReporter.getInstance().reportError(null, "BaseSchemeHandlerFactory.create", "browser is null", mapOf())
            return null
        }

        val project = getProject(browser)
        if (project == null) {
            Log.log(logger::warn, "project is null , should never happen")
            ErrorReporter.getInstance().reportError(null, "BaseSchemeHandlerFactory.create", "project is null", mapOf())
            return null
        }


        val url = getUrl(request)

        if (url != null) {

            val proxyHandler = createProxyHandler(project, url)
            if (proxyHandler != null) {
                return proxyHandler
            }

            val host = url.host
            val file = url.path
            if (getDomain() == host && getSchema() == schemeName) {
                val resourceName = file.removePrefix("/")
                return createResourceHandler(browser, resourceName)
            }
        }
        return null
    }


    private fun getUrl(request: CefRequest): URL? {
        return try {
            URI(request.url).toURL()
        } catch (e: MalformedURLException) {
            null
        }
    }

    protected open fun createProxyHandler(project: Project, url: URL): CefResourceHandler? {

        //check requests to jaeger backend starting with JaegerProxyResourceHandler.JAEGER_UI_API_PATH
        // and proxy them to jaeger backend.
        //or requests to digma api starting with ApiProxyResourceHandler.URL_PREFIX
        // and proxy them to digma backend.
        val jaegerQueryUrl = JaegerProxyResourceHandler.getJaegerQueryUrlOrNull()
        return if (jaegerQueryUrl != null && JaegerProxyResourceHandler.isJaegerQueryCall(url)) {
            JaegerProxyResourceHandler(jaegerQueryUrl)
        }else if (ApiProxyResourceHandler.isApiProxyCall(url)) {
            ApiProxyResourceHandler(project)
        }else{
            null
        }
    }

    abstract fun createResourceHandler(browser: CefBrowser, resourcePath: String): CefResourceHandler
    abstract fun getSchema(): String
    abstract fun getDomain(): String

}
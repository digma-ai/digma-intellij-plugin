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
import java.net.URL


private const val COMMON_FILES_FOLDER: String = "/webview/common"


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
            val host = url.host
            val file = url.file

            val proxyHandler = createProxyHandler(project, url)
            if (proxyHandler != null) {
                return proxyHandler
            }

            if (getDomain() == host && getSchema() == schemeName) {
                var resourceName = getResourceFolderName() + file
                var resource = javaClass.getResource(resourceName)

                if (resource === null) {
                    resourceName = "$COMMON_FILES_FOLDER$file"
                    resource = javaClass.getResource(resourceName)
                }

                return createResourceHandler(resourceName, resource !== null, browser)
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

    protected open fun createProxyHandler(project: Project, url: URL): CefResourceHandler?{
        return null
    }
    abstract fun createResourceHandler(resourceName: String, resourceExists: Boolean, browser: CefBrowser): CefResourceHandler
    abstract fun getSchema(): String
    abstract fun getDomain(): String
    abstract fun getResourceFolderName(): String

}
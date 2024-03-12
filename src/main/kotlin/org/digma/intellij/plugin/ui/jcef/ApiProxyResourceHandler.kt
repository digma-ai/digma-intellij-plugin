package org.digma.intellij.plugin.ui.jcef

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.cef.callback.CefCallback
import org.cef.handler.CefResourceHandler
import org.cef.misc.IntRef
import org.cef.misc.StringRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.lowlevel.HttpRequest
import org.digma.intellij.plugin.model.rest.lowlevel.HttpRequestBody
import org.digma.intellij.plugin.model.rest.lowlevel.HttpResponse
import org.digma.intellij.plugin.settings.SettingsState
import java.net.URL

class ApiProxyResourceHandler(val project: Project) : CefResourceHandler {

    private var apiResponse: HttpResponse? = null

    companion object {
        const val URL_PREFIX = "/api-proxy"
        private val LOGGER = Logger.getInstance(ApiProxyResourceHandler::class.java)
        fun isApiProxyCall(url: URL): Boolean {
            return url.path.startsWith(URL_PREFIX)
        }
    }

    override fun processRequest(request: CefRequest, callback: CefCallback): Boolean {
        try {
            val apiBaseUrl = URL(SettingsState.getInstance().apiUrl)
            val requestUrl = URL(request.url)
            val apiUrl = URL(apiBaseUrl.protocol, apiBaseUrl.host, apiBaseUrl.port, requestUrl.path.removePrefix(URL_PREFIX) + "?" + requestUrl.query)

            val body =
                if (request.postData != null) HttpRequestBody("application/json", request.postData.toString())
                else null
            val headers = mutableMapOf<String, String>()
            request.getHeaderMap(headers)
            val httpRequest = HttpRequest(request.method, apiUrl, headers.toMutableMap(), body)

            apiResponse = AnalyticsService.getInstance(project).lowLevelCall(httpRequest)
            callback.Continue()
            return true
        } catch (e: Throwable) {
            Log.warnWithException(LOGGER, e, "processRequest failed")
            callback.cancel()
            return false
        }
    }

    override fun getResponseHeaders(response: CefResponse, responseLength: IntRef, redirectUrl: StringRef) {
        if (apiResponse == null)
            return

        response.status = apiResponse!!.status
        response.setHeaderMap(apiResponse!!.headers.toMap())
        response.mimeType = apiResponse!!.contentType

        if (apiResponse!!.contentLength != null) {
            responseLength.set(apiResponse!!.contentLength!!.toInt())
        }
    }

    override fun readResponse(dataOut: ByteArray, bytesToRead: Int, bytesRead: IntRef, callback: CefCallback): Boolean {
        try {
            val inputStream = apiResponse?.contentStream
            val read = inputStream?.read(dataOut, 0, bytesToRead)
            if (read == null || read == -1) {
                bytesRead.set(0)
                inputStream?.close()
                return false
            }
            bytesRead.set(read)
            return true
        } catch (e: Exception) {
            Log.warnWithException(logger, e, "exception readResponse")
            throw JCefException(e)
        }
    }

    override fun cancel() {
        apiResponse?.contentStream?.close()
    }
}
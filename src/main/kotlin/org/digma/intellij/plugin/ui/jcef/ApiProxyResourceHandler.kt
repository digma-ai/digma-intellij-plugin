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
import java.net.URI
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
            val apiBaseUrl = URI(SettingsState.getInstance().apiUrl).toURL()
            val requestUrl = URI(request.url).toURL()
            val apiUrl =
                URI(
                    apiBaseUrl.protocol,
                    null,
                    apiBaseUrl.host,
                    apiBaseUrl.port,
                    requestUrl.path.removePrefix(URL_PREFIX),
                    requestUrl.query,
                    null
                ).toURL()

            val body =
                if (request.postData != null) HttpRequestBody("application/json", request.postData.toString())
                else null
            val headers = mutableMapOf<String, String>()
            request.getHeaderMap(headers)
            val httpRequest = HttpRequest(request.method, apiUrl, headers.toMutableMap(), body)

            apiResponse = AnalyticsService.getInstance(project).proxyCall(httpRequest)

            if (apiResponse == null) {
                Log.log(LOGGER::warn, "apiResponse is null , canceling request " + request.url)
                callback.cancel()
                return false
            }

            callback.Continue()
            return true
        } catch (e: Throwable) {
            Log.warnWithException(LOGGER, e, "processRequest failed")
            callback.cancel()
            return false
        }
    }

    override fun getResponseHeaders(response: CefResponse, responseLength: IntRef, redirectUrl: StringRef) {
        apiResponse?.let { res ->
            response.status = res.status
            response.setHeaderMap(res.headers.toMap())
            response.mimeType = res.contentType

            res.contentLength?.let { length ->
                responseLength.set(length.toInt())
            }
        }
    }


    override fun readResponse(dataOut: ByteArray, bytesToRead: Int, bytesRead: IntRef, callback: CefCallback): Boolean {
        return try {
            val inputStream = apiResponse?.contentStream
            val read = inputStream?.read(dataOut, 0, bytesToRead)
            if (read == null || read <= 0) {
                bytesRead.set(0)
                inputStream?.close()
                return false
            }
            bytesRead.set(read)
            true
        } catch (e: Exception) {
            bytesRead.set(0)
            Log.warnWithException(logger, e, "exception readResponse")
            false
        }
    }


    override fun cancel() {
        apiResponse?.contentStream?.close()
    }
}
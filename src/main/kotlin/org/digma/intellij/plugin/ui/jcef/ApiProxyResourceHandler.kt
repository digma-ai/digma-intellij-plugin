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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.settings.SettingsState
import java.net.URL

class ApiProxyResourceHandler(val project: Project) : CefResourceHandler {

    private var apiResponse: Response? = null

    companion object{
        const val URL_PREFIX = "/api-proxy"
        private val LOGGER = Logger.getInstance(ApiProxyResourceHandler::class.java)
        fun isApiProxyCall(url: URL): Boolean{
            return url.path.startsWith(URL_PREFIX)
        }
    }

    override fun processRequest(request: CefRequest, callback: CefCallback): Boolean {
        try {
            val apiBaseUrl = URL(SettingsState.getInstance().apiUrl)
            val requestUrl = URL(request.url)
            val apiUrl = URL(apiBaseUrl.protocol, apiBaseUrl.host, apiBaseUrl.port, requestUrl.path.removePrefix(URL_PREFIX) + "?" + requestUrl.query)
            val apiBody = request.postData?.toString()

            val apiRequest: Request = Request.Builder()
                .method(request.method, apiBody?.toRequestBody("application/json".toMediaType()))
                .url(apiUrl)
                .build()

            apiResponse = AnalyticsService.getInstance(project).lowLevelCall(apiRequest)
            callback.Continue()
            return true
        }
        catch (e: Throwable){
            Log.warnWithException(LOGGER, e, "processRequest failed")
            callback.cancel()
            return false
        }
    }

    override fun getResponseHeaders(response: CefResponse, responseLength: IntRef, redirectUrl: StringRef) {
        if(apiResponse == null)
            return

        response.status = apiResponse!!.code
        response.setHeaderMap(apiResponse!!.headers.toMap())

        val body = apiResponse!!.body
        if(body != null){
            response.mimeType = body.contentType().toString()
            responseLength.set(body.contentLength().toInt())
        }
    }

    override fun readResponse(dataOut: ByteArray, bytesToRead: Int, bytesRead: IntRef, callback: CefCallback): Boolean {
        try {
            val source = apiResponse?.body?.source()
            if(source == null || source.exhausted()){
                bytesRead.set(0)
                apiResponse!!.close()
                return false
            }

            val read = source.read(dataOut, 0, bytesToRead)
            bytesRead.set(read)
            return true
        } catch (e: Exception) {
            Log.warnWithException(logger, e, "exception readResponse")
            throw JCefException(e)
        }
    }

    override fun cancel() {
        apiResponse?.close()
    }
}
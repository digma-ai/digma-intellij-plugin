package org.digma.intellij.plugin.ui.jcef.pluginapi

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.cef.callback.CefCallback
import org.cef.handler.CefResourceHandler
import org.cef.misc.IntRef
import org.cef.misc.StringRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.jcef.hasFileElements
import org.digma.intellij.plugin.ui.jcef.postDataToByteArray
import java.net.URI
import java.net.URL

class PluginApiResourceHandler(val project: Project) : CefResourceHandler {

    private val logger = Logger.getInstance(this::class.java)

    private var apiResponse: PluginApiHttpResponse? = null

    companion object {
        private const val URL_PREFIX = "/plugin-api"

        fun isPluginApiCall(url: URL): Boolean {
            return url.path.startsWith(URL_PREFIX)
        }
    }


    override fun processRequest(request: CefRequest, callback: CefCallback): Boolean {
        Log.log(logger::trace, "processing request {}, [request id:{}]", request.url, request.identifier)

        if ((request.postData?.elementCount ?: -1) > 1) {
            Log.log(
                logger::warn,
                "encountered multi part post data. it is not supported by Digma plugin api handler, [request id:{}]",
                request.identifier
            )

            val error = ErrorData("encountered multi part post data. it is not supported by Digma plugin api handler.")
            apiResponse = PluginApiHttpResponse.createErrorResponse(500, error)
            callback.Continue()
            return true
        }


        if (hasFileElements(request.postData)) {

            //todo: we don't support multi part form data yet or file elements in the post data

            Log.log(
                logger::warn,
                "encountered file element in post data. it is not supported by Digma plugin api handler, [request id:{}]",
                request.identifier
            )

            val error = ErrorData("encountered file element in post data. it is not supported by Digma plugin api handler.")
            apiResponse = PluginApiHttpResponse.createErrorResponse(500, error)
            callback.Continue()
            return true
        }


        //the request is valid only in the scope of this method, so take the data we need before starting a background thread
        val postData = request.postData?.let {
            postDataToByteArray(request, it)
        }
        val requestId = request.identifier
        val requestUrl = request.url
        val requestMethod = request.method
        val headers = mutableMapOf<String, String>()
        request.getHeaderMap(headers)

        Backgroundable.executeOnPooledThread {
            executeRequest(requestId, requestUrl, requestMethod, postData, headers, callback)
        }

        return true
    }


    private fun executeRequest(
        requestId: Long,
        requestUrl: String,
        requestMethod: String,
        postData: ByteArray?,
        headers: Map<String, String>,
        callback: CefCallback
    ) {
        try {

            val apiUrl = URI(requestUrl).toURL()

            apiResponse = getCommand(apiUrl)?.let { command ->
                Log.log(logger::trace, "executing command {}, [request id:{}]", command, requestId)
                command.execute(project, requestId, requestMethod, postData, headers)
            }
            Log.log(logger::trace, "got api response {}, [request id:{}]", apiResponse, requestId)

        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "processRequest {} failed, [request id:{}]", requestUrl, requestId)
            ErrorReporter.Companion.getInstance().reportError("ApiProxyResourceHandler.processRequest", e)

            val error = ErrorData("encountered exception in plugin api handler [$e]. please check the logs.")
            apiResponse = PluginApiHttpResponse.createErrorResponse(500, error)
        } finally {
            callback.Continue()
        }
    }


    private fun getCommand(apiUrl: URL): Command? {
        return Command.getCommand(apiUrl)
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
            ErrorReporter.Companion.getInstance().reportError("ApiProxyResourceHandler.readResponse", e)
            false
        }
    }


    override fun cancel() {
        apiResponse?.contentStream?.close()
    }

}
package org.digma.intellij.plugin.ui.jcef

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.cef.callback.CefCallback
import org.cef.handler.CefResourceHandler
import org.cef.misc.IntRef
import org.cef.misc.StringRef
import org.cef.network.CefPostData
import org.cef.network.CefPostDataElement
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.lowlevel.HttpRequest
import org.digma.intellij.plugin.model.rest.lowlevel.HttpRequestBody
import org.digma.intellij.plugin.model.rest.lowlevel.HttpResponse
import org.digma.intellij.plugin.settings.SettingsState
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Vector

class ApiProxyResourceHandler(val project: Project) : CefResourceHandler {

    private val logger = Logger.getInstance(this::class.java)

    private var apiResponse: HttpResponse? = null

    companion object {
        const val URL_PREFIX = "/api-proxy"

        fun isApiProxyCall(url: URL): Boolean {
            return url.path.startsWith(URL_PREFIX)
        }
    }


    override fun processRequest(request: CefRequest, callback: CefCallback): Boolean {

        Log.log(logger::trace, "processing request {}, [request id:{}]", request.url, request.identifier)

        try {
            val apiBaseUrl = URI(SettingsState.getInstance().apiUrl).toURL()
            val requestUrl = URI(request.url).toURL()
            val apiUrl =
                URI(
                    apiBaseUrl.protocol,
                    null,
                    apiBaseUrl.host,
                    apiBaseUrl.port,
                    requestUrl.path?.let {
                        URLDecoder.decode(it.removePrefix(URL_PREFIX), StandardCharsets.UTF_8)
                    },
                    requestUrl.query?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) },
                    null
                ).toURL()

            Log.log(logger::trace, "proxying to api url {}, [request id:{}]", apiUrl, request.identifier)

            apiResponse = if ((request.postData?.elementCount ?: -1) > 1) {

                //todo: we don't support multi part form data yet

                Log.log(
                    logger::warn,
                    "encountered multi part post data. it is not supported by Digma api proxy, [request id:{}]",
                    request.identifier
                )

                HttpResponse(
                    500,
                    mutableMapOf(),
                    null,
                    "text/plain",
                    "encountered multi part post data. it is not supported by Digma api proxy.".byteInputStream()
                )
            } else if (hasFileElements(request.postData)) {

                //todo: we don't support multi part form data yet or file elements in the post data

                Log.log(
                    logger::warn,
                    "encountered file element in post data. it is not supported by Digma api proxy, [request id:{}]",
                    request.identifier
                )

                HttpResponse(
                    500,
                    mutableMapOf(),
                    null,
                    "text/plain",
                    "encountered file element in post data. it is not supported by Digma api proxy.".byteInputStream()
                )
            } else {
                val body: HttpRequestBody? =
                    request.postData?.let {
                        HttpRequestBody(postDataToByteArray(request, it))
                    }

                Log.log(logger::trace, "body for request is {}, [request id:{}]", body, request.identifier)

                val headers = mutableMapOf<String, String>()
                request.getHeaderMap(headers)
                val httpRequest = HttpRequest(request.method, apiUrl, headers.toMutableMap(), body)

                Log.log(logger::trace, "sending request {}, [request id:{}]", httpRequest, request.identifier)
                AnalyticsService.getInstance(project).proxyCall(httpRequest)
            }

            Log.log(logger::trace, "got api response {}, [request id:{}]", apiResponse, request.identifier)

            if (apiResponse == null) {
                Log.log(logger::warn, "apiResponse is null , canceling request {}, [request id:{}]", request.url, request.identifier)
                callback.cancel()
                return false
            }

            callback.Continue()
            return true
        } catch (e: Throwable) {

            Log.warnWithException(logger, e, "processRequest {} failed, [request id:{}]", request.url, request.identifier)
            ErrorReporter.getInstance().reportError("ApiProxyResourceHandler.processRequest", e)

            apiResponse = HttpResponse(
                500,
                mutableMapOf(),
                null,
                "text/plain",
                "encountered exception in proxy [$e]. please check the logs".byteInputStream()
            )

            callback.Continue()
            return true
        }
    }

    //this method will create a byte array containing all byte arrays from all elements.
    // when calling this method we make sure there are no file elements and that the number of elements not more than 1
    private fun postDataToByteArray(request: CefRequest, postData: CefPostData): ByteArray {

        Log.log(logger::trace, "collecting post data for {}, [request id:{}]", request.url, request.identifier)

        return try {
            val elements = Vector<CefPostDataElement>()
            postData.getElements(elements)

            var allBytes = ByteArray(0)

            elements.forEach { e ->
                val bytesCnt: Int = e.bytesCount
                if (bytesCnt > 0) {
                    val bytes = ByteArray(bytesCnt)
                    e.getBytes(bytes.size, bytes)
                    allBytes = allBytes.plus(bytes)
                    if (logger.isTraceEnabled) {
                        //check if trace enabled because this log message may build large strings
                        Log.log(
                            logger::trace, "collected cef post data from element, data is: {}, all post data is:{}, for {}, [request id:{}]",
                            String(bytes, Charsets.UTF_8),
                            String(allBytes, Charsets.UTF_8),
                            request.url, request.identifier
                        )
                    }
                }
            }

            if (logger.isTraceEnabled) {
                Log.log(
                    logger::trace,
                    "built post data {}, for {}, [request id:{}]",
                    String(allBytes, Charsets.UTF_8),
                    request.url,
                    request.identifier
                )
            }
            allBytes

        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "postDataToByteArray for {} failed, [request id:{}]", request.url, request.identifier)
            ErrorReporter.getInstance().reportError("ApiProxyResourceHandler.postDataToByteArray", e)
            ByteArray(0)
        }
    }


    private fun hasFileElements(postData: CefPostData?): Boolean {
        return postData?.let {
            val elements = Vector<CefPostDataElement>()
            postData.getElements(elements)
            elements.any { it.type == CefPostDataElement.Type.PDE_TYPE_FILE }
        } ?: false
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
            ErrorReporter.getInstance().reportError("ApiProxyResourceHandler.readResponse", e)
            false
        }
    }


    override fun cancel() {
        apiResponse?.contentStream?.close()
    }
}
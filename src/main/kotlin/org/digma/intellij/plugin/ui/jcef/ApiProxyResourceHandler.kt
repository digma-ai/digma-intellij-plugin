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
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.lowlevel.HttpRequest
import org.digma.intellij.plugin.model.rest.lowlevel.HttpRequestBody
import org.digma.intellij.plugin.model.rest.lowlevel.HttpResponse
import org.digma.intellij.plugin.settings.SettingsState
import java.net.URI
import java.net.URL

class ApiProxyResourceHandler(val project: Project) : CefResourceHandler {

    private val logger = Logger.getInstance(this::class.java)

    private var apiResponse: HttpResponse? = null

    companion object {
        const val URL_PREFIX = "/api-proxy"

        fun isApiProxyCall(url: URL): Boolean {
            return url.path.startsWith(URL_PREFIX)
        }

        fun buildApiBaseUrl(): URL {
            return URI(SettingsState.getInstance().apiUrl).toURL()
        }

        /**
         * buildProxyUrl builds the proxied url from base url and the raw request we have from jcef.
         * it should preserve encoding as received from the original url. the resulting url should be
         * encoded the same as the original url we got from jcef.
         */
        fun buildProxyUrl(apiBaseUrl: URL, cefRawRequestUrl: String): URL {

            //This method should be tested.
            //should always be used to build the proxy url when needed,
            //it should make sure to preserve encoding of path and query.
            //see unit test :  org.digma.intellij.plugin.ui.jcef.proxy.ApiProxyTests.testUrls
            //some constructors of URI and URL will encode the path and query, some don't.
            //here we make sure to preserve the path and query as they were received from jcef.
            //we uee URI constructors that don't encode the path and query. there are constructors of
            // URI that will encode the path and query, in that case we will end up with double encoding.
            // one constructor that will validate and encode illegal characters in url and will just encode path and query is
            //java.net.URI.URI(String scheme,
            //               String userInfo, String host, int port,
            //               String path, String query, String fragment)
            //we use a constructor that does not encode.

            //this URI constructor will not encode path and query
            val requestUrl = URI(cefRawRequestUrl).toURL()


            /*
            Implementation note:
            this code will just concatenate some string and build a url.
            it supports everything we need to support now and preserves the original encoding of the
            path and query as received from jcef.
             */
            var apiUrl = apiBaseUrl.toString()
            if (requestUrl.path != null) {
                apiUrl = apiUrl.removeSuffix("/").plus(requestUrl.path.removePrefix(URL_PREFIX))
            }
            if (requestUrl.query != null) {
                apiUrl = apiUrl.plus("?").plus(requestUrl.query)
            }

            return URI(apiUrl).toURL()


            /*
            this constructor of java.net.URI expects that path and query be decoded strings, it will remove illegal url characters
            and replace them , will actually encode the string. if the string is already encoded we will end up with double encoding.
            '%23' will become %2523.
            other constructors of java.net.URI do not behave the same and do not double encode.
            but we still want to use this constructor because we need to build one url from two urls,using the URL class api is comfortable and safe
             because it supports all the http spec.
            our solution is to decode path and query before sending to this constructor.
            and make sure that this code is tested because it may change between JVM versions.
             */

            /*
            Implementation note:
            this code will use a URI constructor that expects the path and query to be legal url characters and will encode
            illegal characters.in order to use this constructor we need to decode path and query, and rely on the constructor that will
            encode them again. but this decoding -> encoding may have edge cases that will not completely preserve the original encoding.
            the advantage of this constructor is that it fully supports the http protocol and will do all the necessary validations.
            todo: currently we don't use this constructor but it can be used with decoding path and query.
             */
//            return URI(
//                apiBaseUrl.protocol,
//                apiBaseUrl.userInfo,
//                apiBaseUrl.host,
//                apiBaseUrl.port,
//                requestUrl.path?.let {
//                    URLDecoder.decode(it.removePrefix(URL_PREFIX), StandardCharsets.UTF_8)
//                },
//                requestUrl.query?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) },
//                null
//            ).toURL()

        }
    }


    override fun processRequest(request: CefRequest, callback: CefCallback): Boolean {
        Log.log(logger::trace, "processing request {}, [request id:{}]", request.url, request.identifier)

        if ((request.postData?.elementCount ?: -1) > 1) {
            Log.log(
                logger::warn,
                "encountered multi part post data. it is not supported by Digma api proxy, [request id:{}]",
                request.identifier
            )

            apiResponse = HttpResponse(
                500,
                mutableMapOf(),
                null,
                "text/plain",
                "encountered multi part post data. it is not supported by Digma api proxy.".byteInputStream()
            )
            callback.Continue()
            return true
        }


        if (hasFileElements(request.postData)) {

            //todo: we don't support multi part form data yet or file elements in the post data

            Log.log(
                logger::warn,
                "encountered file element in post data. it is not supported by Digma api proxy, [request id:{}]",
                request.identifier
            )

            apiResponse = HttpResponse(
                500,
                mutableMapOf(),
                null,
                "text/plain",
                "encountered file element in post data. it is not supported by Digma api proxy.".byteInputStream()
            )
            callback.Continue()
            return true
        }


        //the request is valid only in the scope of this method , so take the data we need before starting a background thread
        val postData = request.postData?.let {
            postDataToByteArray(request, it)
        }
        val requestId = request.identifier
        val requestUrl = request.url
        val requestMethod = request.method
        val headers = mutableMapOf<String, String>()
        request.getHeaderMap(headers)

        Backgroundable.executeOnPooledThread {
            proxyRequest(requestId, requestUrl, requestMethod, postData, headers, callback)
        }

        return true
    }


    private fun proxyRequest(
        requestId: Long,
        requestUrl: String,
        requestMethod: String,
        postData: ByteArray?,
        headers: Map<String, String>,
        callback: CefCallback
    ) {
        try {

            val apiUrl = buildProxyUrl(buildApiBaseUrl(), requestUrl)

            Log.log(logger::trace, "proxying to api url {}, [request id:{}]", apiUrl, requestId)

            val body: HttpRequestBody? =
                postData?.let {
                    HttpRequestBody(postData)
                }

            Log.log(logger::trace, "body for request is {}, [request id:{}]", body, requestId)

            val httpRequest = HttpRequest(requestMethod, apiUrl, headers.toMutableMap(), body)

            Log.log(logger::trace, "sending request {}, [request id:{}]", httpRequest, requestId)
            apiResponse = AnalyticsService.getInstance(project).proxyCall(httpRequest)
            Log.log(logger::trace, "got api response {}, [request id:{}]", apiResponse, requestId)

        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "processRequest {} failed, [request id:{}]", requestUrl, requestId)
            ErrorReporter.getInstance().reportError("ApiProxyResourceHandler.processRequest", e)

            apiResponse = HttpResponse(
                500,
                mutableMapOf(),
                null,
                "text/plain",
                "encountered exception in proxy [$e]. please check the logs".byteInputStream()
            )
        } finally {
            callback.Continue()
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
            ErrorReporter.getInstance().reportError("ApiProxyResourceHandler.readResponse", e)
            false
        }
    }


    override fun cancel() {
        apiResponse?.contentStream?.close()
    }
}
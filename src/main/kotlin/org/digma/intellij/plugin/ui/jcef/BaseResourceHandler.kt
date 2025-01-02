package org.digma.intellij.plugin.ui.jcef

import com.intellij.openapi.diagnostic.Logger
import org.cef.browser.CefBrowser
import org.cef.callback.CefCallback
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefResourceHandler
import org.cef.misc.IntRef
import org.cef.misc.StringRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.updates.ui.UIResourcesService
import java.io.IOException
import java.io.InputStream

abstract class BaseResourceHandler(private val path: String, protected val browser: CefBrowser) : CefResourceHandler {

    val logger = Logger.getInstance(this::class.java)

    private var inputStream: InputStream? = null

    private var resourceType: CefRequest.ResourceType? = null


    private fun isEnvJs(path: String): Boolean {
        return path.equals("${getResourceFolderName()}/env.js", true)
    }

    abstract fun buildEnvJsFromTemplate(path: String): InputStream?
    abstract fun getResourceFolderName(): String

    override fun processRequest(request: CefRequest, callback: CefCallback): Boolean {

        inputStream = if (isEnvJs(path)) {
            buildEnvJsFromTemplate(path)
        } else if (UIResourcesService.getInstance().isResourceExists(path)) {
            UIResourcesService.getInstance().getResourceAsStream(path)
        } else {
            UIResourcesService.getInstance().getResourceAsStream("${getResourceFolderName()}/index.html")
        }

        if (inputStream == null) {
            Log.log(logger::warn, "inputStream is null , canceling request " + request.url)
            callback.cancel()
            return false
        }

        resourceType = request.resourceType
        callback.Continue()
        return true
    }


    override fun getResponseHeaders(response: CefResponse, responseLength: IntRef, redirectUrl: StringRef) {
        if (inputStream == null) {
            response.error = CefLoadHandler.ErrorCode.ERR_FILE_NOT_FOUND
            response.statusText = "file not found $path"
            response.status = 404
            return
        }

        response.status = 200
        response.mimeType = getMimeType()
        try {
            responseLength.set(inputStream!!.available())
        } catch (e: IOException) {
            response.error = CefLoadHandler.ErrorCode.ERR_ABORTED
            response.statusText = "internal error for $path"
            response.status = 500
        }
    }


    private fun getMimeType(): String {
        return when (resourceType) {
            CefRequest.ResourceType.RT_MAIN_FRAME -> {
                "text/html"
            }

            CefRequest.ResourceType.RT_SCRIPT -> {
                "text/javascript"
            }

            CefRequest.ResourceType.RT_STYLESHEET -> {
                "text/css"
            }

            CefRequest.ResourceType.RT_IMAGE -> {
                if (path.endsWith("svg", true)) {
                    "image/svg+xml"
                } else {
                    "image/png"
                }
            }

            else -> {
                "text/plain"
            }
        }
    }


    override fun readResponse(dataOut: ByteArray, bytesToRead: Int, bytesRead: IntRef, callback: CefCallback): Boolean {
        return try {

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
        try {
            inputStream?.close()
        } catch (e: IOException) {
            //ignore
        }
    }
}
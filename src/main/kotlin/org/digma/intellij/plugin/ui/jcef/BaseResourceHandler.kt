package org.digma.intellij.plugin.ui.jcef

import com.intellij.openapi.diagnostic.Logger
import org.cef.callback.CefCallback
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefResourceHandler
import org.cef.misc.IntRef
import org.cef.misc.StringRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import org.digma.intellij.plugin.log.Log
import java.io.IOException
import java.io.InputStream
import kotlin.math.min

abstract class BaseResourceHandler(private val path: String) : CefResourceHandler {

    val logger = Logger.getInstance(this::class.java)

    private var inputStream: InputStream? = null

    private var resourceType: CefRequest.ResourceType? = null


    abstract fun isIndexHtml(path: String): Boolean

    //todo: probably path is not necessary
    abstract fun buildIndexFromTemplate(path: String): InputStream?


    override fun processRequest(request: CefRequest, callback: CefCallback): Boolean {

        inputStream = if (isIndexHtml(path)) {
            buildIndexFromTemplate(path)
        } else {
            javaClass.getResourceAsStream(path)
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

            val available = inputStream!!.available()
            if (available == 0) {
                bytesRead.set(0)
                this.inputStream!!.close()
                return false
            }
            val toRead = min(available.toDouble(), bytesToRead.toDouble()).toInt()
            val read = inputStream!!.read(dataOut, 0, toRead)
            bytesRead.set(read)
            true
        } catch (npe: NullPointerException) {
            //protection against NPE for inputStream, that should not happen unless we have a bug
            Log.warnWithException(logger, npe, "Unexpected NPE in readResponse, probably inputStream is null")
            false
        } catch (e: Exception) {
            Log.warnWithException(logger, e, "exception readResponse")
            throw JCefException(e)
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
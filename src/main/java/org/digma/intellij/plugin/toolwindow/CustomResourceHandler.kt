package org.digma.intellij.plugin.toolwindow

import org.cef.callback.CefCallback
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefResourceHandler
import org.cef.misc.IntRef
import org.cef.misc.StringRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import java.io.IOException
import java.io.InputStream
import java.net.URLConnection
import kotlin.math.min

class CustomResourceHandler : CefResourceHandler {
    private var state: ResourceHandlerState = ClosedConnection
    override fun processRequest(
            cefRequest: CefRequest,
            cefCallback: CefCallback
    ): Boolean {
        val processedUrl = cefRequest.url
        return if (processedUrl != null) {
            val pathToResource = processedUrl.replace("http://myapp", "webview/")
            val newUrl = javaClass.classLoader.getResource(pathToResource)
            if (newUrl != null) {
                state = OpenedConnection(newUrl.openConnection())
            }
            cefCallback.Continue()
            true
        } else {
            false
        }
    }

    override fun getResponseHeaders(
            cefResponse: CefResponse,
            responseLength: IntRef,
            redirectUrl: StringRef
    ): Unit = state.getResponseHeaders(cefResponse, responseLength, redirectUrl)


    override fun readResponse(
            dataOut: ByteArray,
            designedBytesToRead: Int,
            bytesRead: IntRef,
            callback: CefCallback
    ): Boolean = state.readResponse(dataOut, designedBytesToRead, bytesRead, callback)

    override fun cancel(): Unit {
        state.close()
        state = ClosedConnection
    }
}

sealed interface ResourceHandlerState {
    fun getResponseHeaders(
            cefResponse: CefResponse,
            responseLength: IntRef,
            redirectUrl: StringRef
    ): Unit

    fun readResponse(
            dataOut: ByteArray,
    designedBytesToRead: Int,
    bytesRead: IntRef,
    callback: CefCallback
    ): Boolean

    fun close(): Unit
}

data class OpenedConnection(val connection: URLConnection) : ResourceHandlerState {
    private val inputStream: InputStream = connection.getInputStream()
    override fun getResponseHeaders(cefResponse: CefResponse, responseLength: IntRef, redirectUrl: StringRef
    ): Unit {
        try {
            cefResponse.mimeType = URLConnection.guessContentTypeFromName(connection.url.file)
            responseLength.set(inputStream.available())
            cefResponse.status = 200
        } catch ( e: IOException) {
            cefResponse.error = CefLoadHandler.ErrorCode.ERR_FILE_NOT_FOUND
            cefResponse.statusText = e.localizedMessage
            cefResponse.status = 404
        }
    }

    override fun readResponse(
            dataOut: ByteArray,
            designedBytesToRead: Int,
            bytesRead: IntRef,
            callback: CefCallback
    ): Boolean {
        val availableSize = inputStream.available()
        return if (availableSize > 0) {
            val maxBytesToRead = min(availableSize, designedBytesToRead)
            val realNumberOfReadBytes =
                    inputStream.read(dataOut, 0, maxBytesToRead)
            bytesRead.set(realNumberOfReadBytes)
            true
        } else {
            inputStream.close()
            false
        }
    }

    override fun close(): Unit = inputStream.close()

}

object ClosedConnection : ResourceHandlerState {
    override fun getResponseHeaders(
            cefResponse: CefResponse,
            responseLength: IntRef,
            redirectUrl: StringRef
    ): Unit {
        cefResponse.status = 404
    }

    override fun readResponse(
            dataOut: ByteArray,
            designedBytesToRead: Int,
            bytesRead: IntRef,
            callback: CefCallback
    ): Boolean = false

    override fun close() {}
}

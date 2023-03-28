package org.digma.intellij.plugin.toolwindow.common

import freemarker.template.Configuration
import org.cef.callback.CefCallback
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefResourceHandler
import org.cef.misc.IntRef
import org.cef.misc.StringRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.StringWriter
import java.net.URLConnection

const val INDEX_TEMPLATE_FILE: String = "indextemplate.ftl"
const val BASE_PACKAGE_PATH: String = "/webview/"
const val ENV_VARIABLE_THEME: String = "theme"
const val COMMON_FILES_FOLDER_NAME: String = "common"

class CustomResourceHandler(private var resourceFolderName: String) : CefResourceHandler {
    private var state: ResourceHandlerState = ClosedConnection
    override fun processRequest(
            cefRequest: CefRequest,
            cefCallback: CefCallback
    ): Boolean {
        val processedUrl = cefRequest.url
        return if (processedUrl != null) {
            if (processedUrl.equals("http://$resourceFolderName/index.html", true)) {
                val html = loadFreemarkerTemplate(resourceFolderName)
                state = StringData(html)
            } else {
                val pathToResource: String =
                        if (processedUrl.contains("fonts") || processedUrl.contains("images")) {
                            processedUrl.replace("http://$resourceFolderName", "webview/$COMMON_FILES_FOLDER_NAME")
                        } else {
                            processedUrl.replace("http://$resourceFolderName", "webview/$resourceFolderName")
                        }
                val newUrl = javaClass.classLoader.getResource(pathToResource)
                if (newUrl != null) {
                    state = OpenedConnection(newUrl.openConnection())
                }
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
        } catch (e: IOException) {
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
        return CustomResourceHandlerUtil.readResponse(
                inputStream,
                dataOut,
                designedBytesToRead,
                bytesRead,
                callback
        )
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

data class StringData(val data: String) : ResourceHandlerState {
    private val inputStream = ByteArrayInputStream(data.toByteArray(Charsets.UTF_8))

    override fun getResponseHeaders(
            cefResponse: CefResponse,
            responseLength: IntRef,
            redirectUrl: StringRef
    ) {
        cefResponse.mimeType = "text/html"
        responseLength.set(inputStream.available())
        cefResponse.status = 200
    }

    override fun readResponse(
            dataOut: ByteArray,
            designedBytesToRead: Int,
            bytesRead: IntRef,
            callback: CefCallback
    ): Boolean {
        return CustomResourceHandlerUtil.readResponse(
                inputStream,
                dataOut,
                designedBytesToRead,
                bytesRead,
                callback
        )
    }

    override fun close() {
        inputStream.close()
    }
}

private fun loadFreemarkerTemplate(resourceFolderName: String): String {
    val cfg = Configuration(Configuration.VERSION_2_3_30)
    cfg.setClassForTemplateLoading(CustomResourceHandler::class.java, BASE_PACKAGE_PATH + resourceFolderName)
    val template = cfg.getTemplate(INDEX_TEMPLATE_FILE)
    val data = mapOf(ENV_VARIABLE_THEME to ThemeUtil.getCurrentThemeName())
    val writer = StringWriter()
    template.process(data, writer)
    return writer.toString()
}
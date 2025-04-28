package org.digma.intellij.plugin.ui.jcef.pluginapi

import org.digma.intellij.plugin.common.objectToJson
import java.io.InputStream

class PluginApiHttpResponse(
    val status: Int,
    val headers: MutableMap<String, String>,
    val contentLength: Long?,
    val contentType: String?,
    val contentStream: InputStream?
) {
    override fun toString(): String {
        //don't convert contentStream to string, this input stream should be read by the http layer. and it may be a large stream.
        return "PluginApiHttpResponse(status=$status, headers=$headers, contentLength=$contentLength, contentType=$contentType, contentStream=$contentStream)"
    }

    companion object {
        fun createErrorResponse(statusCode: Int, error: ErrorData): PluginApiHttpResponse {
            val errorJson = objectToJson(error).toByteArray()
            return PluginApiHttpResponse(
                statusCode,
                headers = mutableMapOf(
                    "Content-Type" to "application/json",
                    "Content-Length" to errorJson.size.toString()
                ),
                contentLength = errorJson.size.toLong(),
                contentType = "application/json",
                contentStream = errorJson.inputStream()
            )
        }
    }
}
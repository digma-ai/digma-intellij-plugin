package org.digma.intellij.plugin.model.rest.lowlevel

import java.io.InputStream

class HttpResponse(
    val status: Int,
    val headers: MutableMap<String, String>,
    val contentLength: Long?,
    val contentType: String?,
    val contentStream: InputStream?


) {
    override fun toString(): String {
        //don't convert contentStream to string, this input stream should be read by the http layer. and it may be a large stream.
        return "HttpResponse(status=$status, headers=$headers, contentLength=$contentLength, contentType=$contentType, contentStream=$contentStream)"
    }
}

package org.digma.intellij.plugin.model.rest.lowlevel

import java.net.URL

class HttpRequest(
    val method: String,
    val url: URL,
    val headers: MutableMap<String, String>,
    val body: HttpRequestBody?
) {
    fun getHeader(headerName: String): String? {
        val actualHeaderName = headers.keys.find { it.equals(headerName,true) }
        return actualHeaderName?.let {
            headers[it]
        }
    }

    override fun toString(): String {
        return "HttpRequest(method='$method', url=$url, headers=$headers, body=$body)"
    }


}

class HttpRequestBody(
    val content: ByteArray
){
    override fun toString(): String {
        return "HttpRequestBody(content=${String(content, Charsets.UTF_8)})"
    }
}
package org.digma.intellij.plugin.model.rest.lowlevel

import java.net.URL

class HttpRequest(
    val method: String,
    val url: URL,
    val headers: MutableMap<String, String>,
    val body: HttpRequestBody?
)

class HttpRequestBody(
    val contentType: String,
    val content: String
)
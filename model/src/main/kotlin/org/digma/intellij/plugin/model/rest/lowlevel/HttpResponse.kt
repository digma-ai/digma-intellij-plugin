package org.digma.intellij.plugin.model.rest.lowlevel

import java.io.InputStream

class HttpResponse(
    val status: Int,
    val headers: MutableMap<String, String>,
    val contentLength: Long?,
    val contentType: String?,
    val contentStream: InputStream?
)

package org.digma.intellij.plugin.protocol

import java.net.URLEncoder


fun Map<String, String>.toUrlQueryString() =
    this.map {(k,v) -> "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}" }
        .joinToString("&")
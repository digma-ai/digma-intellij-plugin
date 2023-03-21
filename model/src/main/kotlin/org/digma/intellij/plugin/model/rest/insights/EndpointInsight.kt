package org.digma.intellij.plugin.model.rest.insights

interface EndpointInsight : SpanInsight {
    var route: String
    var serviceName: String

    fun endpointSpanName(): String {
        return spanInfo.name
    }
}

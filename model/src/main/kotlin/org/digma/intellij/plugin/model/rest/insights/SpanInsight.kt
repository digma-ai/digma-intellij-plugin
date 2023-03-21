package org.digma.intellij.plugin.model.rest.insights

interface SpanInsight : CodeObjectInsight {
    // scope = "Span"
    val spanInfo: SpanInfo

    fun spanName(): String {
        return spanInfo.name
    }
}

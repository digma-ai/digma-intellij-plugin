package org.digma.intellij.plugin.model.rest.insights

interface SpanInsight : CodeObjectInsight {
    // scope = "Span"
    val spanInfo: SpanInfo

    fun getSpanDisplayName(): String {
        return spanInfo.displayName
    }
}

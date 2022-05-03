@file:JvmName("Models")

package org.digma.intellij.plugin.model

enum class CodeObjectSummaryType {
    MethodSummary, SpanSummary, EndpointSummary
}

//todo: add to CodeObjectInfo
enum class CodeObjectType {
    Method, Span, Endpoint
}


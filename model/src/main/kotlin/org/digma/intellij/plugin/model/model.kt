@file:JvmName("Models")

package org.digma.intellij.plugin.model

enum class CodeObjectSummaryType {
    MethodSummary,
    SpanSummary,
    EndpointSummary,
    Unmapped
}

//todo: add to CodeObjectInfo
enum class CodeObjectType {
    Method, Span, Endpoint
}

enum class ElementUnderCaretType {
    Method
}

enum class InsightType {
    HotSpot,
    Errors,
    SpanUsages,
    SpanDurations,
    SlowestSpans,
    LowUsage,
    NormalUsage,
    HighUsage,
    SlowEndpoint,
    Unmapped,
    TopErrorFlows,
    SpanDurationChange
}

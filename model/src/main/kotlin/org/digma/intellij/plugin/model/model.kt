@file:JvmName("Models")

package org.digma.intellij.plugin.model


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
    EndpointSpaNPlusOne,
    EndpointSessionInView,
    EndpointChattyApi,
    EndpointDurationSlowdown,
    EndpointBreakdown,
    SpaNPlusOne,
    Unmapped,
    TopErrorFlows,
    SpanDurationChange,
    SpanEndpointBottleneck,
    SpanDurationBreakdown,
    SpanScaling,
    SpanScalingRootCause,
    ;
}

enum class InsightImportance(val priority: Int) {
    Spam(9),
    Clutter(8),
    NotInteresting(7),
    Info(6),

    Interesting(5),
    Important(4),

    HighlyImportant(3),
    Critical(2),
    ShowStopper(1)
}

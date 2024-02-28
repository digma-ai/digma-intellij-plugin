package org.digma.intellij.plugin.model

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue


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
    EndpointHighNumberOfQueries,
    EndpointQueryOptimization,
    SpaNPlusOne,
    TopErrorFlows,
    SpanDurationChange,
    SpanEndpointBottleneck,
    SpanDurationBreakdown,
    SpanScaling,
    SpanScalingRootCause,
    SpanNexus,
    SpanQueryOptimization,
    SpanScalingInsufficientData,
    SpanScalingWell,
    @JsonEnumDefaultValue
    Unmapped

}

enum class InsightImportance(val priority: Int) {
    HCF(0),
    ShowStopper(1),
    Critical(2),
    HighlyImportant(3),
    Important(4),
    Interesting(5),
    Info(6),
    NotInteresting(7),
    Clutter(8),
    Spam(9)
}

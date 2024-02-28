package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

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
package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanNPlusEndpoints
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("endpointInfo", "occurrences", "severity", "impact", "criticality", "traceId", "duration")
constructor(
        val endpointInfo: EndpointInfo,
        val occurrences: Number,
        val severity: Double,
        val impact: Double,
        val criticality: Double,
        val traceId: String,
        val duration: Duration
)
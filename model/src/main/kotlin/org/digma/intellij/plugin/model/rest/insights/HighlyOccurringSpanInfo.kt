package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class HighlyOccurringSpanInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("occurrences", "internalSpan", "clientSpan", "traceId", "duration", "fraction", "severity", "impact", "criticality")
constructor(
        val occurrences: Double,
        val internalSpan: SpanInfo?,
        val clientSpan: SpanInfo?,
        val traceId: String?,
        val duration: Duration,
        val fraction: Double,
        val severity: Double,
        val impact: Double,
        val criticality: Double
)


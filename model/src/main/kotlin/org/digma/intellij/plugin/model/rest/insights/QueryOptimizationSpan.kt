package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class QueryOptimizationSpan
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("spanInfo", "traceId", "severity", "criticality", "ticketLink", "duration")
constructor(
    val spanInfo: SpanInfo?,
    val traceId: String?,
    val severity: Double,
    val criticality: Double,
    var ticketLink: String?,
    val duration: Duration,
)
package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties
import java.sql.Timestamp

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanDurationsPercentile
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("percentile", "currentDuration", "previousDuration", "changeTime", "changeVerified", "traceIds")
constructor(
    val percentile: Float,
    val currentDuration: Duration,
    val previousDuration: Duration?,
    val changeTime: Timestamp?,
    val changeVerified: Boolean?,
    val traceIds: List<String>?
)

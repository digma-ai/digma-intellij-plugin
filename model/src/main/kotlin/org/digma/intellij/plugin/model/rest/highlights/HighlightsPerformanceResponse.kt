package org.digma.intellij.plugin.model.rest.highlights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.common.Duration
import org.digma.intellij.plugin.model.rest.common.SpanDurationsPercentile
import org.digma.intellij.plugin.model.rest.common.SpanDurationsPlot
import java.beans.ConstructorProperties
import java.sql.Timestamp

@JsonIgnoreProperties(ignoreUnknown = true)
data class HighlightsPerformanceResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "environment",
    "percentiles",
    "lastSpanInstanceInfo",
    "histogramPlot"
)
constructor(
    val environment: String,
    val percentiles: Array<SpanDurationsPercentile>,
    val lastSpanInstanceInfo: SpanInstanceInfo?,
    val histogramPlot: SpanDurationsPlot?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanInstanceInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "traceId",
    "spanId",
    "startTime",
    "duration"
)
constructor(
    var traceId: String?,
    var spanId: String?,
    var startTime: Timestamp?,
    var duration: Duration?
)
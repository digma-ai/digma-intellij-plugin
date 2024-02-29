package org.digma.intellij.plugin.model.rest.common

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
    val traceIds: List<String>?,
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanDurationsPlot
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("bars", "quantiles")
constructor(
    val bars: List<SpanDurationsPlotBar>,
    val quantiles: List<SpanDurationsPlotQuantileMarker>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanDurationsPlotBar
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("index", "count", "start", "end")
constructor(
    val index: Int,
    val count: Long,
    val start: Duration,
    val end: Duration,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanDurationsPlotQuantileMarker
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("quantileValue", "timestamp")
constructor(
    val quantileValue: Float,
    val timestamp: Duration,
)

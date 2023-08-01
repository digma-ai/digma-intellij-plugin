package org.digma.intellij.plugin.model.rest.version

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties
import java.time.Duration
import java.util.Date

@JsonIgnoreProperties(ignoreUnknown = true)
data class PerformanceMetricsResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "metrics",
    "serverStartTime",
//    "serverAliveTime",
    "probeTime",
)
constructor(val metrics: List<PerformanceCounterReport>,
            val serverStartTime: Date,
//            val serverAliveTime: Duration,
            val probeTime: Date)


@JsonIgnoreProperties(ignoreUnknown = true)
data class PerformanceCounterReport
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "metric",
    "value",
)
constructor(val metric: String,
            val value: Any)
package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SlowEndpointInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("endpointInfo", "p50", "p95", "p99", "probabilityOfBeingBottleneck", "avgDurationWhenBeingBottleneck")
constructor(
    val endpointInfo: EndpointInfo,
    val p50: Percentile, // Obsolete
    val p95: Percentile, // Obsolete
    val p99: Percentile, // Obsolete
    val ProbabilityOfBeingBottleneck: Double?,
    val AvgDurationWhenBeingBottleneck: Duration?
)

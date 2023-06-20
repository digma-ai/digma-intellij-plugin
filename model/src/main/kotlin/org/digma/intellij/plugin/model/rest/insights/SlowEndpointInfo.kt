package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SlowEndpointInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("endpointInfo", "probabilityOfBeingBottleneck", "avgDurationWhenBeingBottleneck")
constructor(
    val endpointInfo: EndpointInfo,
    val ProbabilityOfBeingBottleneck: Double,
    val AvgDurationWhenBeingBottleneck: Duration
)

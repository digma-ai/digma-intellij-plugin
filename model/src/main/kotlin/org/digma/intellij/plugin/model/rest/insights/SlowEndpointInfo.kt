package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SlowEndpointInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("endpointInfo", "p50", "p95", "p99")
constructor(
    val endpointInfo: EndpointInfo,
    val p50: Percentile,
    val p95: Percentile,
    val p99: Percentile,
)

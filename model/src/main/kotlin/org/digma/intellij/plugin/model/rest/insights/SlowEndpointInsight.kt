package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties

data class SlowEndpointInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "codeObjectId", "route",
    "endpointsMedian", "endpointsMedianOfMedians", "endpointsMedianOfP75", "endpointsP75",
    "min", "max", "mean", "median",
    "p75", "p95", "p99",
)
constructor(
    override val codeObjectId: String,
    override var route: String,
    val endpointsMedian: Duration,
    val endpointsMedianOfMedians: Duration,
    val endpointsMedianOfP75: Duration,
    val endpointsP75: Duration,
    val min: Duration,
    val max: Duration,
    val mean: Duration,
    val median: Duration,
    val p75: Duration,
    val p95: Duration,
    val p99: Duration
) : EndpointInsight {
    override val type: InsightType = InsightType.SlowEndpoint
}

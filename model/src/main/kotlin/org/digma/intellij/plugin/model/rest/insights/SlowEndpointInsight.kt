package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.Date

@JsonIgnoreProperties(ignoreUnknown = true)
data class SlowEndpointInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
        "codeObjectId",
        "environment",
        "scope",
        "importance",
        "decorators",
        "actualStartTime",
        "customStartTime",
        "prefixedCodeObjectId",
        "isRecalculateEnabled",
        "shortDisplayInfo",
        "spanInfo",
        "route",
        "serviceName",
        "endpointsMedian",
        "endpointsMedianOfMedians",
        "endpointsMedianOfP75",
        "endpointsP75",
        "min",
        "max",
        "mean",
        "median",
        "p75",
        "p95",
        "p99",
)
constructor(
        override val codeObjectId: String,
        override val environment: String,
        override val scope: String,
        override val importance: Int,
        override val decorators: List<CodeObjectDecorator>?,
        override val actualStartTime: Date?,
        override val customStartTime: Date?,
        override val prefixedCodeObjectId: String?,
        override val isRecalculateEnabled: Boolean,
        override val shortDisplayInfo: ShortDisplayInfo?,
        override val spanInfo: SpanInfo,
        override var route: String,
        override var serviceName: String,
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
        val p99: Duration,
) : EndpointInsight {
    override val type: InsightType = InsightType.SlowEndpoint
}

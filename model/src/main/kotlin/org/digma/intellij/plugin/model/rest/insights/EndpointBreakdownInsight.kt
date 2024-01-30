package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.Date

@JsonIgnoreProperties(ignoreUnknown = true)
data class EndpointBreakdownInsight
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
    "severity",
    "impact",
    "criticality",
    "firstCommitId",
    "lastCommitId",
    "deactivatedCommitId",
    "reopenCount",
    "ticketLink",
    "firstDetected",
    "lastDetected"
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
    @get:JsonProperty("isRecalculateEnabled")
    @param:JsonProperty("isRecalculateEnabled")
    override val isRecalculateEnabled: Boolean,
    override val shortDisplayInfo: ShortDisplayInfo?,
    override val spanInfo: SpanInfo,
    override var route: String,
    override var serviceName: String,
    override val severity: Double,
    override val impact: Double,
    override val criticality: Double,
    override val firstCommitId: String?,
    override val lastCommitId: String?,
    override val deactivatedCommitId: String?,
    override val reopenCount: Int,
    override val ticketLink: String?,
    override val firstDetected: Date?,
    override val lastDetected: Date?,

    ) : EndpointInsight {
    override val type: InsightType = InsightType.EndpointBreakdown

    // hasAsyncSpans. default is false.
    //  when value false should refer to fraction (percents)
    //  when value is true should refer to duration
    @JsonProperty("hasAsyncSpans")
    val hasAsyncSpans: Boolean = false

    // components - deprecated. instead, use p50Components
    @JsonProperty("components")
    val components: List<EndpointBreakdownComponent> = emptyList()

    @JsonProperty("p50Components")
    val p50Components: List<EndpointBreakdownComponent> = emptyList()

    @JsonProperty("p95Components")
    val p95Components: List<EndpointBreakdownComponent> = emptyList()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class EndpointBreakdownComponent
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("type", "fraction")
constructor(
    val type: String,
    val fraction: Double,
) {
    // duration - temporary nullable. but won't be null when hasAsyncSpans is true
    @JsonProperty("duration")
    val duration: Duration? = null
}


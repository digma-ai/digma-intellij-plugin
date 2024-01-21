package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.Date

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanDurationsInsight
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
    //  "isRecalculateEnabled",
    "shortDisplayInfo",
    "spanInfo",
    "severity",
    "impact",
    "criticality",
    "firstCommitId",
    "lastCommitId",
    "deactivatedCommitId",
    "reopenCount",
    "ticketLink"
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
    override val shortDisplayInfo: ShortDisplayInfo?,
    override val spanInfo: SpanInfo,
    override val severity: Double,
    override val impact: Double,
    override val criticality: Double,
    override val firstCommitId: String?,
    override val lastCommitId: String?,
    override val deactivatedCommitId: String?,
    override val reopenCount: Int,
    override val ticketLink: String?,
) : SpanInsight {

    override val type: InsightType = InsightType.SpanDurations

    @JsonProperty(value = "isRecalculateEnabled")
    override val isRecalculateEnabled: Boolean = true // should remove the setter = true later ...support backward compatibility

    @JsonProperty("percentiles")
    val percentiles: List<SpanDurationsPercentile> = emptyList()

    @JsonProperty("lastSpanInstanceInfo")
    val lastSpanInstanceInfo: SpanInstanceInfo? = null

    @JsonProperty("histogramPlot")
    val histogramPlot: SpanDurationsPlot? = null

    @JsonProperty("average")
    val average: Duration? = null

    @JsonProperty("standardDeviation")
    val standardDeviation: Duration? = null

    // isAsync means this span ends later than its parent (this.EndTime > parent.EndTime)
    @JsonProperty("isAsync")
    val isAsync: Boolean = false
}

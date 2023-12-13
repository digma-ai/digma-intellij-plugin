package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.Date

@JsonIgnoreProperties(ignoreUnknown = true)
data class EndpointDurationSlowdownInsight
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
    "durationSlowdownSources",
    "severity",
    "impact",
    "criticality"
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
    val durationSlowdownSources: List<DurationSlowdownSource>,
    override val severity: Double,
    override val impact: Double,
    override val criticality: Double
) : EndpointInsight {
    override val type: InsightType = InsightType.EndpointDurationSlowdown
}
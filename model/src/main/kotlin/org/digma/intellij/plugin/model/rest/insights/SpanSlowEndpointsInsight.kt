package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.Date

// TODO: rename to SpanEndpointsBottleneckInsight
@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanSlowEndpointsInsight
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
        "slowEndpoints",
        "severity",
        "impact",
        "criticality",
        "firstCommitId",
        "lastCommitId",
        "deactivatedCommitId",
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
        val slowEndpoints: List<SlowEndpointInfo>,
        override val severity: Double,
        override val impact: Double,
        override val criticality: Double,
        override val firstCommitId: String?,
        override val lastCommitId: String?,
        override val deactivatedCommitId: String?,
) : SpanInsight {
    override val type: InsightType = InsightType.SpanEndpointBottleneck
}
package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.Date

@JsonIgnoreProperties(ignoreUnknown = true)
class SpanNPlusOneInsight
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
        "traceId",
        "clientSpanName",
        "clientSpanCodeObjectId",
        "occurrences",
        "duration",
        "endpoints",
        "severity",
        "impact",
        "criticality",
        "firstCommitId",
        "lastCommitId",
        "deactivatedCommitId",
        "reopenCount",
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
        val traceId: String?,
        val clientSpanName: String?,
        val clientSpanCodeObjectId: String?,
        val occurrences: Number,
        val duration: Duration,
        val endpoints: List<SpanNPlusEndpoints>,
        override val severity: Double,
        override val impact: Double,
        override val criticality: Double,
        override val firstCommitId: String?,
        override val lastCommitId: String?,
        override val deactivatedCommitId: String?,
        override val reopenCount: Int,
) : SpanInsight {
    override val type: InsightType = InsightType.SpaNPlusOne
}
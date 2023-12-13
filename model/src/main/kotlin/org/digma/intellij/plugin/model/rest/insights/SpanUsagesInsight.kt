package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.Date

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanUsagesInsight
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
        "span",
        "flows",
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
        val span: String,
        val flows: List<SpanFlow>,
        override val severity: Double,
        override val impact: Double,
        override val criticality: Double
) : SpanInsight {

    override val type: InsightType = InsightType.SpanUsages
}
package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.Date
import kotlin.collections.ArrayList

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanScalingRootCauseInsight
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
        "affectedEndpoints",
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
        val affectedEndpoints: List<AffectedEndpointInfo> = ArrayList()
) : SpanInsight {

    override val type: InsightType = InsightType.SpanScalingRootCause
}
package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.InsightImportance
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.*

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
        "shortDisplayInfo",
        "span",
        "slowEndpoints"
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
        val span: SpanInfo,
        val slowEndpoints: List<SlowEndpointInfo>,
) : CodeObjectInsight {
    override val type: InsightType = InsightType.SpanEndpointBottleneck
}
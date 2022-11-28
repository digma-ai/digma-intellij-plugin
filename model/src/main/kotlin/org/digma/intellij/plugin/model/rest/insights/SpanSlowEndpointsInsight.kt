package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanSlowEndpointsInsight@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId", "actualStartTime", "customStartTime", "prefixedCodeObjectId", "span", "slowEndpoints")
constructor(
        override val codeObjectId: String,
        override val actualStartTime: Date?,
        override val customStartTime: Date?,
        override val prefixedCodeObjectId: String?,
        val span: SpanInfo,
        val slowEndpoints: List<SlowEndpointInfo>,
) : CodeObjectInsight {
    override val type: InsightType = InsightType.SpanEndpointBottleneck
}
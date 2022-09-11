package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanSlowEndpointsInsight@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId", "span", "slowEndpoints")
constructor(
    override val codeObjectId: String,
    val span: SpanInfo,
    val slowEndpoints: List<SlowEndpointInfo>,
) : CodeObjectInsight {
    override val type: InsightType = InsightType.SpanEndpointBottleneck
}
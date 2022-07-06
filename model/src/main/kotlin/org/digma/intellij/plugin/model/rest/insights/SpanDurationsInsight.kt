package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties

data class SpanDurationsInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId", "span", "percentiles")
constructor(
    override val codeObjectId: String,
    val span: SpanInfo,
    val percentiles: List<SpanDurationsPercentile>
) : CodeObjectInsight {

    override val type: InsightType = InsightType.SpanDurations
}
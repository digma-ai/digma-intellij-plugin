package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.*

data class SpanDurationsInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId", "actualStartTime", "customStartTime", "prefixedCodeObjectId", "span", "percentiles")
constructor(
        override val codeObjectId: String,
        override val actualStartTime: Date?,
        override val customStartTime: Date?,
        override val prefixedCodeObjectId: String?,
        val span: SpanInfo,
        val percentiles: List<SpanDurationsPercentile>,
) : CodeObjectInsight {

    override val type: InsightType = InsightType.SpanDurations
}
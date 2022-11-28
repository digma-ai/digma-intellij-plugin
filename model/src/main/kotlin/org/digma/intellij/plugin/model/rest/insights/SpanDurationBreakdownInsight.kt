package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.*

data class SpanDurationBreakdownInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId", "actualStartTime", "customStartTime", "prefixedCodeObjectId", "spanName", "breakdownEntries")
constructor(
        override val codeObjectId: String,
        override val actualStartTime: Date?,
        override val customStartTime: Date?,
        override val prefixedCodeObjectId: String?,
        val spanName: String,
        val breakdownEntries: List<SpanDurationBreakdown>,
) : CodeObjectInsight {

    override val type: InsightType = InsightType.SpanDurationBreakdown
}
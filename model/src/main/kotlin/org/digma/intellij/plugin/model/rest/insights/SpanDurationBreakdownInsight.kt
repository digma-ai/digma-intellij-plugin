package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties

data class SpanDurationBreakdownInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId", "spanName", "breakdownEntries")
constructor(
    override val codeObjectId: String,
    val spanName: String,
    val breakdownEntries: List<SpanDurationBreakdown>
) : CodeObjectInsight {

    override val type: InsightType = InsightType.SpanDurationBreakdown
}
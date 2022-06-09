package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties

data class SlowestSpansInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId", "route", "spans")
constructor(
    override val codeObjectId: String,
    override var route: String,
    val spans: List<SlowSpanInfo>
) : EndpointInsight {
    override val type: InsightType = InsightType.SlowestSpans
}
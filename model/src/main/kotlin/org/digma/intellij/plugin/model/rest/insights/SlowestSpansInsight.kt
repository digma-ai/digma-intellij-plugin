package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.*

data class SlowestSpansInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId", "route", "actualStartTime", "customStartTime", "prefixedCodeObjectId", "endpointSpan", "spans")
constructor(
        override val codeObjectId: String,
        override var route: String,
        override val actualStartTime: Date?,
        override val customStartTime: Date?,
        override val prefixedCodeObjectId: String?,
        override var endpointSpan: String,
        val spans: List<SlowSpanInfo>,
) : EndpointInsight {
    override val type: InsightType = InsightType.SlowestSpans
}
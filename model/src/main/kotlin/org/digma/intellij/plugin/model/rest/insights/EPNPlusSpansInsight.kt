package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.*

data class EPNPlusSpansInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
        "codeObjectId",
        "environment",
        "scope",
        "importance",
        "decorators",
        "route",
        "endpointSpan",
        "actualStartTime",
        "customStartTime",
        "prefixedCodeObjectId",
        "spans",
)
constructor(
        override val codeObjectId: String,
        override val environment: String,
        override val scope: String,
        override val importance: Int,
        override val decorators: List<CodeObjectDecorator>?,
        override var route: String,
        override var endpointSpan: String,
        override val actualStartTime: Date?,
        override val customStartTime: Date?,
        override val prefixedCodeObjectId: String?,
        val spans: List<HighlyOccurringSpanInfo>,
) : EndpointInsight {
        override val type: InsightType = InsightType.EndpointSpaNPlusOne
}

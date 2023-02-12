package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.*

class SpanNPlusOneInsight
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
        "traceId",
        "span",
        "clientSpanName",
        "occurrences",
        "duration",
        "endpoints"
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
        val traceId: String?,
        val span: SpanInfo,
        val clientSpanName: String?,
        val occurrences: Number,
        val duration: Duration,
        val endpoints: List<SpanNPlusEndpoints>,
) : CodeObjectInsight {
    override val type: InsightType = InsightType.SpaNPlusOne
}
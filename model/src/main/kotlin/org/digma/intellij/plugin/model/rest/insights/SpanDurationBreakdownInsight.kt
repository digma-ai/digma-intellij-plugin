package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.Date

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanDurationBreakdownInsight
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
      //  "isRecalculateEnabled",
        "shortDisplayInfo",
        "spanInfo",
        "spanName",
        "breakdownEntries"
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
        //  override val isRecalculateEnabled: Boolean, // should enable this one when removing the default true assignment
        override val shortDisplayInfo: ShortDisplayInfo?,
        override val spanInfo: SpanInfo,
        val spanName: String,
        val breakdownEntries: List<SpanDurationBreakdown>
) : SpanInsight {

    override val type: InsightType = InsightType.SpanDurationBreakdown
    override val isRecalculateEnabled: Boolean = true // should remove the setter = true later ...support backward compatibility

}
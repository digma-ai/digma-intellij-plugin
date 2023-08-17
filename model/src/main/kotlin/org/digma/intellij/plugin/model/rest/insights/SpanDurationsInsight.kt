package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.Date

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanDurationsInsight
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
    override val spanInfo: SpanInfo,
) : SpanInsight {

    override val type: InsightType = InsightType.SpanDurations
    override val isRecalculateEnabled: Boolean = true // should remove the setter = true later ...support backward compatibility

    @JsonProperty("percentiles")
    val percentiles: List<SpanDurationsPercentile> = emptyList()

    @JsonProperty("lastSpanInstanceInfo")
    val lastSpanInstanceInfo: SpanInstanceInfo? = null

    // isAsync means this span ends later than its parent (this.EndTime > parent.EndTime)
    @JsonProperty("isAsync")
    val isAsync: Boolean = false
}

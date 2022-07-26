package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties

data class SpanDurationChangeInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("spanDurationChanges")
constructor(val spanDurationChanges: List<Change>) : GlobalInsight {

    override val type: InsightType = InsightType.SpanDurationChange
    override fun count(): Int = spanDurationChanges.size

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Change
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    @ConstructorProperties("codeObjectId", "span", "percentiles")
    constructor(
            val codeObjectId: String?,
            val span: SpanInfo,
            val percentiles: List<SpanDurationsPercentile>
    )
}
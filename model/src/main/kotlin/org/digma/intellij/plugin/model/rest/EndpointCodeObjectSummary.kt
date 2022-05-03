package org.digma.intellij.plugin.model.rest

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.CodeObjectSummaryType
import java.beans.ConstructorProperties

data class EndpointCodeObjectSummary
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId", "insightsCount", "errorsCount", "highUsage", "lowUsage", "maxCallsIn1Min")
constructor(
    override val codeObjectId: String,
    override val insightsCount: Int = 0,
    override val errorsCount: Int = 0,
    val highUsage: Boolean = false,
    val lowUsage: Boolean = false,
    val maxCallsIn1Min: Int = 0
) : CodeObjectSummary {

    override val type: CodeObjectSummaryType = CodeObjectSummaryType.EndpointSummary
}
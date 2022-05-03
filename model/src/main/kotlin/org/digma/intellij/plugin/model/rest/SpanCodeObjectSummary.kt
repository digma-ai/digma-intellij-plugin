package org.digma.intellij.plugin.model.rest

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.CodeObjectSummaryType
import java.beans.ConstructorProperties

data class SpanCodeObjectSummary
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId", "insightsCount", "errorsCount")
constructor(
    override val codeObjectId: String,
    override val insightsCount: Int = 0,
    override val errorsCount: Int = 0
) : CodeObjectSummary {

    override val type: CodeObjectSummaryType = CodeObjectSummaryType.SpanSummary
}

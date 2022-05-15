package org.digma.intellij.plugin.model.rest.summary

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.CodeObjectSummaryType
import java.beans.ConstructorProperties

data class MethodCodeObjectSummary
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId", "insightsCount", "errorsCount", "score", "executedCodes")
constructor(
    override val codeObjectId: String,
    override val insightsCount: Int = 0,
    override val errorsCount: Int = 0,
    val score: Int = 0,
    val executedCodes: List<ExecutedCodeSummary>
) : CodeObjectSummary {

    override val type: CodeObjectSummaryType = CodeObjectSummaryType.MethodSummary
}

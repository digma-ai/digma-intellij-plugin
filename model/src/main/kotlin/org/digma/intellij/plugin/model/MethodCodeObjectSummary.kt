package org.digma.intellij.plugin.model

import com.fasterxml.jackson.annotation.JsonCreator
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

    override val type: CodeObjectType = CodeObjectType.MethodSummary
}

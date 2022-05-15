package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties

data class ErrorInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId", "errorCount", "unhandledCount", "unexpectedCount", "topErrors")
constructor(
    override val codeObjectId: String,
    val errorCount: Int = 0,
    val unhandledCount: Int = 0,
    val unexpectedCount: Int = 0,
    val topErrors: List<ErrorInsightNamedError>
) : CodeObjectInsight {

    override val type: InsightType = InsightType.Errors
}

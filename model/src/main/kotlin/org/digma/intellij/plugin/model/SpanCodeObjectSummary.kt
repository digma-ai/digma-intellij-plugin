package org.digma.intellij.plugin.model

import com.fasterxml.jackson.annotation.JsonCreator
import java.beans.ConstructorProperties

data class SpanCodeObjectSummary
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId", "insightsCount", "errorsCount")
constructor(
    override val codeObjectId: String,
    override val insightsCount: Int = 0,
    override val errorsCount: Int = 0
) : CodeObjectSummary {

    override val type: CodeObjectType = CodeObjectType.SpanSummary
}

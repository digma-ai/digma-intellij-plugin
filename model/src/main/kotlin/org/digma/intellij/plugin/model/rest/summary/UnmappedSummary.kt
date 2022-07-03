package org.digma.intellij.plugin.model.rest.summary

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.model.CodeObjectSummaryType
import java.beans.ConstructorProperties

data class UnmappedSummary
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId","type")
constructor(
    override val codeObjectId: String,
    @JsonProperty("type")
    val theType: String,
    override val insightsCount: Int = 0,
    override val errorsCount: Int = 0
) : CodeObjectSummary{

    override val type: CodeObjectSummaryType = CodeObjectSummaryType.Unmapped
}

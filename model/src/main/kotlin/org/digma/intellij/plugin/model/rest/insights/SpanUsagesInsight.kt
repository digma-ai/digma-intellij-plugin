package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties

data class SpanUsagesInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId", "span", "flows")
constructor(
    override val codeObjectId: String,
    val span: String,
    val flows: List<SpanFlow>
) : CodeObjectInsight {

    override val type: InsightType = InsightType.SpanUsages
}
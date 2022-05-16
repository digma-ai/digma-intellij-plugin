package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties

data class HighUsageInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId", "route", "maxCallsIn1Min")
constructor(
    override val codeObjectId: String,
    val route: String,
    val maxCallsIn1Min: Int
) : CodeObjectInsight {
    override val type: InsightType = InsightType.HighUsage
}

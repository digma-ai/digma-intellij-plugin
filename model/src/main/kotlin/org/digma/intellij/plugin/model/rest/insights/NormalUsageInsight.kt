package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties


data class NormalUsageInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId", "maxCallsIn1Min","route")
constructor(
    override val codeObjectId: String,
    val maxCallsIn1Min: Int,
    val route: String
) : CodeObjectInsight {

    override val type: InsightType = InsightType.NormalUsage
}

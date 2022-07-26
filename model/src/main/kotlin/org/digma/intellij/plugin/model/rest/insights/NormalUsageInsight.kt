package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties

data class NormalUsageInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeObjectId", "route", "maxCallsIn1Min")
constructor(
    override val codeObjectId: String,
    override var route: String,
    val maxCallsIn1Min: Int
) : EndpointInsight {
    override val type: InsightType = InsightType.NormalUsage
}

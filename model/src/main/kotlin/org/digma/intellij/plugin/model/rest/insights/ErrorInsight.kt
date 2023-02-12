package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.InsightImportance
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.*

data class ErrorInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
        "codeObjectId",
        "environment",
        "scope",
        "importance",
        "decorators",
        "actualStartTime",
        "customStartTime",
        "prefixedCodeObjectId",
        "shortDisplayInfo",
        "errorCount",
        "unhandledCount",
        "unexpectedCount",
        "topErrors"
)
constructor(
        override val codeObjectId: String,
        override val environment: String,
        override val scope: String,
        override val importance: Int,
        override val decorators: List<CodeObjectDecorator>?,
        override val actualStartTime: Date?,
        override val customStartTime: Date?,
        override val prefixedCodeObjectId: String?,
        override val shortDisplayInfo: ShortDisplayInfo?,
        val errorCount: Int = 0,
        val unhandledCount: Int = 0,
        val unexpectedCount: Int = 0,
        val topErrors: List<ErrorInsightNamedError>,
) : CodeObjectInsight {

    override val type: InsightType = InsightType.Errors
}

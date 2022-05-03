package org.digma.intellij.plugin.model.rest

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.digma.intellij.plugin.model.CodeObjectSummaryType

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = MethodCodeObjectSummary::class, name = "MethodSummary"),
    JsonSubTypes.Type(value = SpanCodeObjectSummary::class, name = "SpanSummary")
)
interface CodeObjectSummary {
    val type: CodeObjectSummaryType
    val codeObjectId: String
    val insightsCount: Int
    val errorsCount: Int
}



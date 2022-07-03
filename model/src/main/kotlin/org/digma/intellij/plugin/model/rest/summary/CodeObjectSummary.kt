package org.digma.intellij.plugin.model.rest.summary

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.digma.intellij.plugin.model.CodeObjectSummaryType

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true, defaultImpl = UnmappedSummary::class)
@JsonSubTypes(
    JsonSubTypes.Type(value = MethodCodeObjectSummary::class, name = "MethodSummary"),
    JsonSubTypes.Type(value = SpanCodeObjectSummary::class, name = "SpanSummary"),
    JsonSubTypes.Type(value = EndpointCodeObjectSummary::class, name = "EndpointSummary")
)
interface CodeObjectSummary {
    val type: CodeObjectSummaryType
    val codeObjectId: String
    val insightsCount: Int
    val errorsCount: Int
}



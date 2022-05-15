package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.digma.intellij.plugin.model.InsightType

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = HotspotInsight::class, name = "HotSpot"),
    JsonSubTypes.Type(value = ErrorInsight::class, name = "Errors"),
    JsonSubTypes.Type(value = SpanInsight::class, name = "SpanUsages"),
)
interface CodeObjectInsight {
    val type: InsightType
    val codeObjectId: String
}
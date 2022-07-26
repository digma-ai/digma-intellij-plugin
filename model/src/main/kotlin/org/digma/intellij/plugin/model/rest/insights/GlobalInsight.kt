package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.digma.intellij.plugin.model.InsightType


/**
 * Mapping for insights types.
 * this mapping supports unmapped types with UnmappedInsight class. JsonIgnoreProperties is necessary only for
 * unmapped types and makes the mapping less robust. the property 'type' is visible only to be mapped to UnmappedInsight
 * so that it can be showed in the GUI but is not mapped in any other subtype, instead there is an enum type.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true, defaultImpl = UnmappedInsight::class)
@JsonSubTypes(
    JsonSubTypes.Type(value = TopErrorFlowsInsight::class, name = "TopErrorFlows"),
    JsonSubTypes.Type(value = SpanDurationChangeInsight::class, name = "SpanDurationChange"),
)
interface GlobalInsight {
    val type: InsightType
    fun count(): Int
}
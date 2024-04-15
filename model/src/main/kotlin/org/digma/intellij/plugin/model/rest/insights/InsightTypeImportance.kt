package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class InsightTypesForJaegerResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "spanCodeObjectId",
    "insights"
)
constructor(
    val spanCodeObjectId: String,
    val insights: List<InsightTypeImportance>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InsightTypeImportance
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "type",
    "importance",
    "criticality"
)
constructor(
    val type: String,
    val importance: Int,
    val criticality: Number,
)

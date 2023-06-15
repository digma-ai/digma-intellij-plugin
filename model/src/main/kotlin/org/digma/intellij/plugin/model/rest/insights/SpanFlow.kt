package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanFlow
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("percentage", "intermediateSpan", "lastServiceSpan", "firstService", "lastService", "sampleTraceIds")
constructor(
    val percentage: Float,
    val intermediateSpan: String?,
    val lastServiceSpan: String?,
    val firstService: Service?,
    val lastService: Service?,
    val sampleTraceIds: List<String>,
) {

    data class Service
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    @ConstructorProperties("service", "span", "codeObjectId","spanCodeObjectId")
    constructor(
        val service: String,
        val span: String,
        val codeObjectId: String,
        val spanCodeObjectId: String
    )

}
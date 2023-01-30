package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanNPlusEndpoints
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("endpointInfo", "occurrences")
constructor(
        val endpointInfo: EndpointInfo,
        val occurrences: Number
)
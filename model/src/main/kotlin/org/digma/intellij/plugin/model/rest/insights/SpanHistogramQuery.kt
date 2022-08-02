package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanHistogramQuery @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) @ConstructorProperties(
    "environment",
    "spanName",
    "instrumentationLibrary",
    "codeObjectId"
) constructor(
    val environment: String,
    val spanName: String,
    val instrumentationLibrary: String,
    val codeObjectId: String,
)

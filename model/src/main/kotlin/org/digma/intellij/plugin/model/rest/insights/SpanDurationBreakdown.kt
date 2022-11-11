package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanDurationBreakdown
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("spanName", "spanDisplayName", "spanInstrumentationLibrary", "spanCodeObjectId", "percentiles")
constructor(
    val spanName: String,
    val spanDisplayName: String,
    val spanInstrumentationLibrary: String,
    val spanCodeObjectId: String,
    val percentiles: List<SpanDurationBreakdownPercentile>
)
package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanDurationBreakdownPercentile
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("percentile", "duration")
constructor(
        val percentile: Float,
        val duration: Duration,
)
package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties
import java.util.Date

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanInstanceInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "traceId",
    "spanId",
    "startTime",
    "duration",
)
constructor(
    val traceId: String,
    val spanId: String,
    val startTime: Date,
    val duration: Duration,
)

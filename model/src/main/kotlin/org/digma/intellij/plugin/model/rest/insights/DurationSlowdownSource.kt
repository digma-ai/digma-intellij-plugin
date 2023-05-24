package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties
import java.sql.Timestamp

@JsonIgnoreProperties(ignoreUnknown = true)
data class DurationSlowdownSource
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "percentile",
    "spanInfo",
    "level",
    "previousDuration",
    "currentDuration",
    "changeTime",
    "changeVerified",
)
constructor(
    val percentile: String,
    val spanInfo: SpanInfo,
    val level: Int,
    val previousDuration: Duration,
    val currentDuration: Duration,
    val changeTime: Timestamp?,
    val changeVerified: Boolean?,
)
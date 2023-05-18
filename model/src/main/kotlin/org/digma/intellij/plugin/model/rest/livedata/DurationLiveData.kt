package org.digma.intellij.plugin.model.rest.livedata

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.insights.Duration
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsInsight
import java.beans.ConstructorProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class LiveDataRecord
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "duration", "dateTime"
)
constructor(val duration: Duration, val dateTime: String)


data class DurationLiveData
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "liveDataRecords",
    "durationInsight"
)
constructor(val liveDataRecords: List<LiveDataRecord>,val durationInsight: SpanDurationsInsight?)


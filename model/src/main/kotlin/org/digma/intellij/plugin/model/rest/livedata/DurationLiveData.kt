package org.digma.intellij.plugin.model.rest.livedata

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.model.rest.common.Duration
import java.beans.ConstructorProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class LiveDataDurationPercentile
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "percentile", "currentDuration","previousDuration","changeVerified"
)
constructor(val percentile: Double, val currentDuration: Duration, val previousDuration: Duration?, val changeVerified: Boolean?)




@JsonIgnoreProperties(ignoreUnknown = true)
data class DurationData
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "percentiles", "codeObjectId","displayName"
)
constructor(val percentiles: List<LiveDataDurationPercentile>,val codeObjectId: String,val displayName: String)



@JsonIgnoreProperties(ignoreUnknown = true)
data class LiveDataRecord
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "duration", "dateTime", "isError"
)
constructor(val duration: Duration,
            val dateTime: String,
            @get:JsonProperty("isError")
            @param:JsonProperty("isError")
            val isError: Boolean)


data class DurationLiveData
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "liveDataRecords",
    "durationData"
)
constructor(val liveDataRecords: List<LiveDataRecord>,val durationData: DurationData?)


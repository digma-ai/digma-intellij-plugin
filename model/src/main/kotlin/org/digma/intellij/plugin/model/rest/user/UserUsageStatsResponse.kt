package org.digma.intellij.plugin.model.rest.user

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.beans.ConstructorProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class UserUsageStatsResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("accountId")
constructor(
    val accountId: String,
) {
    @JsonProperty("environmentsCount")
    val environmentsCount: Int = 0

    @JsonProperty("uniqueSpansCount")
    val uniqueSpansCount: Int = 0

    @JsonProperty("uniqueDatabaseSpansCount")
    val uniqueDatabaseSpansCount: Int = 0

    @JsonProperty("traceDepthAvg")
    val traceDepthAvg: Double = 0.0

    @JsonProperty("traceDepthMax")
    val traceDepthMax: Int = 0

    @JsonProperty("servicesCount")
    val servicesCount: Int = 0

    @JsonProperty("hasDistributedCalls")
    val hasDistributedCalls: Boolean = false
}

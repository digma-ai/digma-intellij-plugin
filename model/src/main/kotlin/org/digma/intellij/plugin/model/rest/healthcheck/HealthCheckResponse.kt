package org.digma.intellij.plugin.model.rest.healthcheck

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.insights.Duration
import java.beans.ConstructorProperties


enum class HealthCheckStatus { Healthy, Unhealthy, Degraded }

@JsonIgnoreProperties(ignoreUnknown = true)
data class HealthCheckResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "name",
    "status",
    "duration",
    "info"
)
constructor(
    val name: String,
    val status: HealthCheckStatus,
    val duration: Duration,
    val info: List<HealthCheckInfo>,
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class HealthCheckInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "key",
    "description",
    "duration",
    "status",
    "error"
)
constructor(
    val key: String,
    val description: String,
    val duration: Duration,
    val status: HealthCheckStatus,
    val error: String,
)


package org.digma.intellij.plugin.recentactivity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResponseEntry


data class JcefMessagePayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val environments: List<String>, val entries: List<RecentActivityResponseEntry>)

data class JcefConnectionCheckMessagePayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val result: String)

data class JcefDockerResultPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val result: String, val error: String)

enum class ConnectionCheckResult(val value: String) {
    SUCCESS("success"),
    FAILURE("failure")
}


data class JcefDockerIsDigmaEngineInstalledPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(
    @get:JsonProperty("isDigmaEngineInstalled")
    @param:JsonProperty("isDigmaEngineInstalled")
    val isDigmaEngineInstalled: Boolean,
)

data class JcefDockerIsDigmaEngineRunningPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(
    @get:JsonProperty("isDigmaEngineRunning")
    @param:JsonProperty("isDigmaEngineRunning")
    val isDigmaEngineRunning: Boolean,
)

data class JcefDockerIsDockerInstalledPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(
    @get:JsonProperty("isDockerInstalled")
    @param:JsonProperty("isDockerInstalled")
    val isDockerInstalled: Boolean,
)

data class JcefDockerIsDockerComposeInstalledPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(
    @get:JsonProperty("isDockerComposeInstalled")
    @param:JsonProperty("isDockerComposeInstalled")
    val isDockerComposeInstalled: Boolean,
)


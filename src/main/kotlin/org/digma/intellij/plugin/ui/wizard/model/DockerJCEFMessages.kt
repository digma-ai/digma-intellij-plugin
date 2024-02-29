package org.digma.intellij.plugin.ui.wizard.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.beans.ConstructorProperties


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


@JsonIgnoreProperties(ignoreUnknown = true)
data class JcefDockerResultRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("type", "action", "payload")
constructor(
    val type: String?,
    val action: String,
    val payload: JcefDockerResultPayload?,
)

data class JcefDockerResultPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val result: String, val error: String)


@JsonIgnoreProperties(ignoreUnknown = true)
data class JcefDockerIsDigmaEngineInstalledRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("type", "action", "payload")
constructor(
    val type: String?,
    val action: String,
    val payload: JcefDockerIsDigmaEngineInstalledPayload?,
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class JcefDockerIsDigmaEngineRunningRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("type", "action", "payload")
constructor(
    val type: String?,
    val action: String,
    val payload: JcefDockerIsDigmaEngineRunningPayload?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class JcefDockerIsDockerInstalledRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("type", "action", "payload")
constructor(
    val type: String?,
    val action: String,
    val payload: JcefDockerIsDockerInstalledPayload?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class JcefDockerIsDockerComposeInstalledRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("type", "action", "payload")
constructor(
    val type: String?,
    val action: String,
    val payload: JcefDockerIsDockerComposeInstalledPayload?,
)


package org.digma.intellij.plugin.jcef.common

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


data class JcefDockerResultRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("type", "action", "payload")
constructor(
    val type: String?,
    val action: String,
    val payload: JcefDockerResultPayload?,
)


data class JcefDockerIsDigmaEngineInstalledRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("type", "action", "payload")
constructor(
    val type: String?,
    val action: String,
    val payload: JcefDockerIsDigmaEngineInstalledPayload?,
)


data class JcefDockerIsDigmaEngineRunningRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("type", "action", "payload")
constructor(
    val type: String?,
    val action: String,
    val payload: JcefDockerIsDigmaEngineRunningPayload?,
)

data class JcefDockerIsDockerInstalledRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("type", "action", "payload")
constructor(
    val type: String?,
    val action: String,
    val payload: JcefDockerIsDockerInstalledPayload?,
)

data class JcefDockerIsDockerComposeInstalledRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("type", "action", "payload")
constructor(
    val type: String?,
    val action: String,
    val payload: JcefDockerIsDockerComposeInstalledPayload?,
)


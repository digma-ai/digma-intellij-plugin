package org.digma.intellij.plugin.recentactivity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties


data class JcefMessageRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("type", "action", "payload")
constructor(
        val type: String?,
        val action: String,
        val payload: JcefMessagePayload?
)

data class JcefConnectionCheckMessageRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("type", "action", "payload")
constructor(
        val type: String?,
        val action: String,
        val payload: JcefConnectionCheckMessagePayload?,
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





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

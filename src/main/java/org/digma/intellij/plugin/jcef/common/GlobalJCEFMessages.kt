package org.digma.intellij.plugin.jcef.common

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.beans.ConstructorProperties

data class IsObservabilityEnabledMessageRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("type", "action", "isObservabilityEnabled")
constructor(
    val type: String,
    val action: String,
    val payload: IsObservabilityEnabledPayload,
)

data class IsObservabilityEnabledPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(
    @get:JsonProperty("isObservabilityEnabled")
    @param:JsonProperty("isObservabilityEnabled")
    val isObservabilityEnabled: Boolean,
)

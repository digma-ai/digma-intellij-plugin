package org.digma.intellij.plugin.ui.wizard.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SetObservabilityRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("action", "payload")
constructor(
    val action: String,
    val payload: SetObservabilityRequestPayload?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SetObservabilityRequestPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("isObservabilityEnabled")
constructor(
    @get:JsonProperty("isObservabilityEnabled")
    @param:JsonProperty("isObservabilityEnabled")
    val isObservabilityEnabled: Boolean,
)
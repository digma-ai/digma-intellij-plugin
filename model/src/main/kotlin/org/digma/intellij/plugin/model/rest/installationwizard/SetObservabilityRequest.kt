package org.digma.intellij.plugin.model.rest.installationwizard

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.beans.ConstructorProperties

data class SetObservabilityRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("action", "payload")
constructor(
    val action: String,
    val payload: SetObservabilityRequestPayload?
)

data class SetObservabilityRequestPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("isObservabilityEnabled")
constructor(
        @get:JsonProperty("isObservabilityEnabled")
        val isObservabilityEnabled: Boolean
)
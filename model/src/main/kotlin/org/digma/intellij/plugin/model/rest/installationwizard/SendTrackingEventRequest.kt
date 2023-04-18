package org.digma.intellij.plugin.model.rest.installationwizard

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

data class SendTrackingEventRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("action", "payload")
constructor(
    val action: String,
    val payload: SendTrackingEventRequestPayload?
)

data class SendTrackingEventRequestPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("eventName", "data")
constructor(
    val eventName: String,
    val data: Map<String, Any>
)
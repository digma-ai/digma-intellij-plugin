package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SendTrackingEventRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("action", "payload")
constructor(
    val action: String,
    val payload: SendTrackingEventRequestPayload?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SendTrackingEventRequestPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("eventName", "data")
constructor(
    val eventName: String,
    val data: Map<String, Any>?,
)
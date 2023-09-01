package org.digma.intellij.plugin.ui.notifications.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode


//@JsonIgnoreProperties(ignoreUnknown = true)
//@JsonInclude(JsonInclude.Include.NON_NULL)
class SetNotificationsMessage
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val type: String, val action: String, val payload: JsonNode)
//constructor(
//    @JsonProperty(value = "type", required = true) val type: String,
//    @JsonProperty(value = "action", required = true) val action: String,
//    @JsonProperty(value = "payload", required = true) val payload: JsonNode,
//)

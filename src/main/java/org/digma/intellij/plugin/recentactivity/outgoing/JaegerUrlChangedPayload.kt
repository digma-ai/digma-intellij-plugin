package org.digma.intellij.plugin.recentactivity.outgoing

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty


data class JaegerUrlChangedPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
constructor(
        @get:JsonProperty("isJaegerEnabled")
        val isJaegerEnabled: Boolean
)
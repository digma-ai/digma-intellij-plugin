package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty


//todo: rename
data class JaegerUrlChangedPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
constructor(
        @get:JsonProperty("isJaegerEnabled")
        @param:JsonProperty("isJaegerEnabled")
        val isJaegerEnabled: Boolean,
)
package org.digma.intellij.plugin.ui.jaegerui.model

import com.fasterxml.jackson.annotation.*
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpansMessage
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("action", "payload")
constructor(
    val action: String,
    val payload: SpanListPayload
)
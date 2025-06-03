package org.digma.intellij.plugin.ui.jaegerui.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class SpansWithResolvedLocationMessage
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("type", "action", "payload")
constructor(
    val type: String,
    val action: String,
    val payload: Map<String, SpanData>
)
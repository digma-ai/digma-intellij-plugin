package org.digma.intellij.plugin.ui.jaegerui.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanListPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("spans")
constructor(
    val spans: List<Span>
)
package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SessionInViewSpanInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties( "renderSpan", "clientSpan", "traceId")
constructor(
    val renderSpan: SpanInfo?,
    val clientSpan: SpanInfo?,
    val traceId: String?,
)
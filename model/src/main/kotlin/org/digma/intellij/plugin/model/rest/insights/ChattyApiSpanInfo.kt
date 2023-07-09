package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ChattyApiSpanInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(  "clientSpan", "traceId", "repeats")
constructor(
    val clientSpan: SpanInfo?,
    val traceId: String?,
    val repeats: Int
)
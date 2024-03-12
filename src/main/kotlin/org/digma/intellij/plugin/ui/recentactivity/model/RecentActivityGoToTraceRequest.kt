package org.digma.intellij.plugin.ui.recentactivity.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.recentactivity.EntrySpan
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class RecentActivityGoToTraceRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("action", "payload")
constructor(
    val action: String,
    val payload: RecentActivityEntrySpanForTracePayload?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RecentActivityEntrySpanForTracePayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("traceId", "span")
constructor(
    val traceId: String,
    val span: EntrySpan,
)
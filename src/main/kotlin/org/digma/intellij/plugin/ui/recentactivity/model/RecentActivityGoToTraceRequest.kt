package org.digma.intellij.plugin.ui.recentactivity.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.recentactivity.EntrySpan
import java.beans.ConstructorProperties

data class RecentActivityGoToTraceRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("action", "payload")
constructor(
    val action: String,
    val payload: RecentActivityEntrySpanForTracePayload?,
)

data class RecentActivityEntrySpanForTracePayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("traceId", "span")
constructor(
    val traceId: String,
    val span: EntrySpan,
)
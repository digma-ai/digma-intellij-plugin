package org.digma.intellij.plugin.ui.recentactivity.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.recentactivity.EntrySpan
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class RecentActivityGoToSpanRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("action", "payload")
constructor(
    val action: String,
    val payload: RecentActivityEntrySpanPayload?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RecentActivityEntrySpanPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("span", "environment")
constructor(
    val span: EntrySpan,
    val environment: String,
)

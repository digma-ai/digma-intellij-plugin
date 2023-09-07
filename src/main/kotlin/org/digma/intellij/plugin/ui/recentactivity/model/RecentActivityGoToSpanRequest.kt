package org.digma.intellij.plugin.ui.recentactivity.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.recentactivity.EntrySpan
import java.beans.ConstructorProperties

data class RecentActivityGoToSpanRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("action", "payload")
constructor(
    val action: String,
    val payload: RecentActivityEntrySpanPayload?,
)

data class RecentActivityEntrySpanPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("span")
constructor(
    val span: EntrySpan,
    val environment: String,
)

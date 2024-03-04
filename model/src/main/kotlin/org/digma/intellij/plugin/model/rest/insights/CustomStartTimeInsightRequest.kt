package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator

data class CustomStartTimeInsightRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(
        val environment: String,
        val id: String,
        val time: String,
)
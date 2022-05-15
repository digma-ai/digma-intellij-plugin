package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator

data class InsightsRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val environment: String, val codeObjectIds: List<String>)

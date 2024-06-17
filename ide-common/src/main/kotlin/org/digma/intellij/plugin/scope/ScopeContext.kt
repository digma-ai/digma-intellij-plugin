package org.digma.intellij.plugin.scope

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.JsonNode
import java.beans.ConstructorProperties

data class ScopeContext
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "event",
    "payload"
)
constructor(
    val event: String? = null,
    val payload: JsonNode? = null
)

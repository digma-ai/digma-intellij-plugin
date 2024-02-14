package org.digma.intellij.plugin.model.rest.codelens

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MethodWithCodeLens
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "methodCodeObjectId",
    "isAlive",
    "decorators"
)
constructor(
    val methodCodeObjectId: String,
    val isAlive: Boolean,
    val decorators: Array<Decorator>
)

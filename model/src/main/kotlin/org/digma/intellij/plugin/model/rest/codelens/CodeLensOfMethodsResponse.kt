package org.digma.intellij.plugin.model.rest.codelens

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CodeLensOfMethodsResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "methodWithCodeLens"
)
constructor(
    val methodWithCodeLens: List<MethodWithCodeLens>
)
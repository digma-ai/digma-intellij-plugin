package org.digma.intellij.plugin.model.rest.codelens

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.insights.MethodWithCodeObjects
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CodeLensOfMethodsRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "environment",
    "methods"
)
constructor(
    val environment: String,
    val methods: List<MethodWithCodeObjects>
)
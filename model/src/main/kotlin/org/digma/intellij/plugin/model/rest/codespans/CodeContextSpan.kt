package org.digma.intellij.plugin.model.rest.codespans

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class CodeContextSpan
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "spanCodeObjectId",
    "displayName",
    "serviceName"
)
constructor(val spanCodeObjectId: String, val displayName: String, val serviceName: String?)

package org.digma.intellij.plugin.model.rest.env

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class DeleteEnvironmentResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "environmentName",
    "success"
)
constructor(val environmentName: String, val success: Boolean)

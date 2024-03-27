package org.digma.intellij.plugin.model.rest.recentactivity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.common.Duration
import java.beans.ConstructorProperties
import java.util.Date


@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateEnvironmentRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("environment", "type")
constructor(
    val type: String,
    val environment: String,
)

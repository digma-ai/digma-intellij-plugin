package org.digma.intellij.plugin.model.rest.recentactivity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.common.Duration
import java.beans.ConstructorProperties
import java.util.Date


@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvironmentCreatedResult
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("id")
constructor(
    val id: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvironmentErrorResult
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("code","description")
constructor(
    val id: String,
    val description: String,
)


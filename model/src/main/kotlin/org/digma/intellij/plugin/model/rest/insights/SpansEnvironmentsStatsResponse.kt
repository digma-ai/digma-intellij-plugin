package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.environment.Env
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanEnvironment
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "environment",
    "issueCounts"
)
constructor(
    val environment: Env,
    val issueCounts: IssueCounts
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class IssueCounts
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "highCriticality",
    "mediumCriticality",
    "lowCriticality"
)
constructor(
    val highCriticality: Int,
    val mediumCriticality: Int,
    val lowCriticality: Int
)
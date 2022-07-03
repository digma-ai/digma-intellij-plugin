package org.digma.intellij.plugin.model.rest.errors

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties
@JsonIgnoreProperties(ignoreUnknown = true)
data class ScoreInfo @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("score", "scoreParams")
constructor(
    val score: Int,
    val scoreParams: Map<String, Int>,
)
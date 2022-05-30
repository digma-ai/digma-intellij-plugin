package org.digma.intellij.plugin.model.rest.errors

import com.fasterxml.jackson.annotation.JsonCreator
import java.beans.ConstructorProperties

data class ScoreInfo @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("score", "scoreParams")
constructor(
    val score: Int,
    val scoreParams: Map<String, Int>,
)
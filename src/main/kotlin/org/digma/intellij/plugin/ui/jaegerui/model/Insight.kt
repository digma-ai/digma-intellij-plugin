package org.digma.intellij.plugin.ui.jaegerui.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class Insight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("type", "importance")
constructor(
    val type: String,
    val importance: Int
)
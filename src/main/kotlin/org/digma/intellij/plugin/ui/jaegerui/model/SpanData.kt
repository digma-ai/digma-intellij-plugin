package org.digma.intellij.plugin.ui.jaegerui.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.beans.ConstructorProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanData
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("hasCodeLocation", "insights")
constructor(
    @get:JsonProperty("hasCodeLocation")
    @param:JsonProperty("hasCodeLocation")
    val hasCodeLocation: Boolean,
    val insights: MutableList<Insight>
)

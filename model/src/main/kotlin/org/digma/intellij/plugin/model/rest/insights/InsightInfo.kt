package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class InsightInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "codeObjectId",
    "type",
    "importance"
)
constructor(
    val codeObjectId: String,
    val type: String,
    @JsonSetter(nulls = Nulls.SKIP)
    val importance: Int = 0,
)

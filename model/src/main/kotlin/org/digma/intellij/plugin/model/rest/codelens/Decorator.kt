package org.digma.intellij.plugin.model.rest.codelens

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.InsightImportance
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Decorator
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "codeObjectId",
    "title",
    "description",
    "importance"
)
constructor(
    val codeObjectId: String,
    val title: String,
    val description: String,
    val importance: InsightImportance
)

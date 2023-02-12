package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ShortDisplayInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "title",
    "targetDisplayName",
    "subtitle",
    "description",
)
constructor(
    val title: String?,
    val targetDisplayName: String?,
    val subtitle: String?,
    val description: String?
)
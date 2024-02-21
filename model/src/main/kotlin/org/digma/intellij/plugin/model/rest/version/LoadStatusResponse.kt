package org.digma.intellij.plugin.model.rest.version

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties
import java.util.Date

@JsonIgnoreProperties(ignoreUnknown = true)
data class LoadStatusResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "description",
    "lastUpdated",
    "occurredRecently"
)
constructor(
    val description: String?,
    val lastUpdated: Date,
    val occurredRecently: Boolean,
)
package org.digma.intellij.plugin.model.rest.version

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties
import java.util.Date

@JsonIgnoreProperties(ignoreUnknown = true)
data class LoadStatusResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "loadType",
    "lastOccurred",
    "occurredRecently"
)
constructor(val loadType: String,
            val lastOccurred: Date,
            val occurredRecently: Boolean)
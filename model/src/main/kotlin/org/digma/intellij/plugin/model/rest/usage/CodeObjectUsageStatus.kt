package org.digma.intellij.plugin.model.rest.usage

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties
import java.sql.Timestamp

data class CodeObjectUsageStatus
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("environment", "type", "name", "codeObjectId", "firstRecordedTime", "lastRecordedTime")
constructor(
    val environment: String,
    val type: String, // CodeObject, Endpoint, Span
    val name: String,
    val codeObjectId: String,
    val firstRecordedTime: Timestamp,
    val lastRecordedTime: Timestamp,
)

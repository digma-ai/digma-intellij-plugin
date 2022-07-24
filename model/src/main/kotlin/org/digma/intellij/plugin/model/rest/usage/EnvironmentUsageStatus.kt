package org.digma.intellij.plugin.model.rest.usage

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties
import java.sql.Timestamp

data class EnvironmentUsageStatus
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("name", "environmentFirstRecordedTime", "environmentLastRecordedTime")
constructor(
    val name: String,
    val environmentFirstRecordedTime: Timestamp,
    val environmentLastRecordedTime: Timestamp,
)

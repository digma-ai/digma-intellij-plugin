package org.digma.intellij.plugin.model.rest.usage

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

data class EnvUsageStatusResult
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("codeObjectStatuses", "environmentStatuses")
constructor(
    val codeObjectStatuses: List<CodeObjectUsageStatus>,
    val environmentStatuses: List<EnvironmentUsageStatus>,
)

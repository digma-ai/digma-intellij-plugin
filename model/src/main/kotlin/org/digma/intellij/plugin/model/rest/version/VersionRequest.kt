package org.digma.intellij.plugin.model.rest.version

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class VersionRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(
    val pluginVersion: String,
    val platformType: String,
    val platformVersion: String,
)
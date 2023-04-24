package org.digma.intellij.plugin.model.rest.version

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class VersionResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "plugin",
    "backend",
)
constructor(
    val plugin: PluginVersionResponse,
    val backend: BackendVersionResponse,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PluginVersionResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "isNewVersionAvailable",
    "latestVersion",
)
constructor(
    val isNewVersionAvailable: Boolean = false,
    val latestVersion: String? = "",
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BackendVersionResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "isNewVersionAvailable",
    "latestVersion",
    "currentVersion",
    "deploymentType",
)
constructor(
    val isNewVersionAvailable: Boolean = false,
    val latestVersion: String? = "",
    val currentVersion: String? = "",
    val deploymentType: BackendDeploymentType = BackendDeploymentType.Unknown,
)

enum class BackendDeploymentType {
    SAAS,
    DockerCompose,
    DockerExtension,

    @JsonEnumDefaultValue
    Unknown,
}
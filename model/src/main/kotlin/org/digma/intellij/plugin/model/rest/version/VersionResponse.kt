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
    "ui",
    "errors",
    "forceUpdate"
)
constructor(
    val plugin: PluginVersionResponse,
    val backend: BackendVersionResponse,
    val ui: UIVersionResponse? = null,
    val errors: List<String>,
    val forceUpdate: ForceUpdate? = null
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

@JsonIgnoreProperties(ignoreUnknown = true)
data class UIVersionResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "isNewMatchingVersionAvailable",
    "latestMatchingVersion"
)
constructor(
    val isNewMatchingVersionAvailable: Boolean = false,
    val latestMatchingVersion: String? = ""
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class ForceUpdate
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "minPluginVersionRequired",
    "minBackendVersionRequired",
    "minUiVersionRequired"
)
constructor(val minPluginVersionRequired: String?, val minBackendVersionRequired: String?, val minUIVersionRequired: String?)


enum class BackendDeploymentType {
    SAAS,
    Helm,
    DockerCompose,
    DockerExtension,

    @JsonEnumDefaultValue
    Unknown,
}
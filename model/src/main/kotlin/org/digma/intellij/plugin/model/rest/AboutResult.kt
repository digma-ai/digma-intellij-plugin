package org.digma.intellij.plugin.model.rest

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import java.beans.ConstructorProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class AboutResult @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("applicationVersion","deploymentType")
constructor(
    val applicationVersion: String,
    val deploymentType: BackendDeploymentType ?= BackendDeploymentType.Unknown
)
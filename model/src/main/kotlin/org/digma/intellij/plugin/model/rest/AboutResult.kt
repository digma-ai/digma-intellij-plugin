package org.digma.intellij.plugin.model.rest

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import java.beans.ConstructorProperties


const val UNKNOWN_APPLICATION_VERSION = "unknown"

@JsonIgnoreProperties(ignoreUnknown = true)
data class AboutResult @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("applicationVersion", "deploymentType", "isCentralize", "site", "features")
constructor(
    val applicationVersion: String,
    val deploymentType: BackendDeploymentType? = BackendDeploymentType.Unknown,
    val isCentralize: Boolean?,
    val site: String?,
    val features: Map<String, String>? = null
) {

    companion object {
        @JvmStatic
        val UNKNOWN = AboutResult(UNKNOWN_APPLICATION_VERSION, BackendDeploymentType.Unknown, false, null,null)
    }
}
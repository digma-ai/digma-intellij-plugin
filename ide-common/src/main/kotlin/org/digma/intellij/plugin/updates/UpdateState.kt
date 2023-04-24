package org.digma.intellij.plugin.updates

import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType

data class UpdateState(
    val backendDeploymentType: BackendDeploymentType,
    val shouldUpdateBackend: Boolean,
    val shouldUpdatePlugin: Boolean,
) {

    fun shouldUpdateAny(): Boolean {
        return shouldUpdateBackend || shouldUpdatePlugin
    }

}

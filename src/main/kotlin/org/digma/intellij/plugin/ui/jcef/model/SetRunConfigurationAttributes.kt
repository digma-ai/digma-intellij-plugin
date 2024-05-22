package org.digma.intellij.plugin.ui.jcef.model

import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class SetRunConfigurationAttributesMessage(val payload: RunConfigurationAttributesPayload) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = JCEFGlobalConstants.GLOBAL_SET_RUN_CONFIGURATION
}

data class RunConfigurationAttributesPayload(
    val environmentId: String?,
    val environmentName: String?,
    val environmentType: String?,
    val userId: String?,
    val observabilityMode: RunConfigObservabilityMode?
)

enum class RunConfigObservabilityMode { Micrometer, OtelAgent }


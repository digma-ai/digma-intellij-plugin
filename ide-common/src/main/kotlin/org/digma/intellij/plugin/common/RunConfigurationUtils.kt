package org.digma.intellij.plugin.common

import com.intellij.execution.CommonProgramRunConfigurationParameters
import com.intellij.execution.configuration.AbstractRunConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration


//this is usually to extract the value of otel resource attributes as a map
// don't call it for something else as it will not work
fun getEnvironmentMapFromRunConfiguration(config: RunConfiguration): Map<String, String>? {
    return when (config) {
        is CommonProgramRunConfigurationParameters -> {
            config.envs
        }

        is ExternalSystemRunConfiguration -> {
            config.settings.env
        }

        is AbstractRunConfiguration -> {
            config.envs
        }

        else -> {
            null
        }
    }

}

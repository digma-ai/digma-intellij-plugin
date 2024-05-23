package org.digma.intellij.plugin.common

import com.intellij.execution.CommonProgramRunConfigurationParameters
import com.intellij.execution.configuration.AbstractRunConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import org.digma.intellij.plugin.buildsystem.BuildSystemHelperService
import org.digma.intellij.plugin.externalsystem.findMavenService


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
            tryGetFromMaven(config)
        }
    }

}

fun tryGetFromMaven(config: RunConfiguration): Map<String, String>? {
    val mavenService: BuildSystemHelperService? = findMavenService()
    return mavenService?.getEnvironmentMapFromRunConfiguration(config)
}

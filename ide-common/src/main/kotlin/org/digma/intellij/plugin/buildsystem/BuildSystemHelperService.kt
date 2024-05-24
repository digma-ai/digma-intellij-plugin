package org.digma.intellij.plugin.buildsystem

import com.intellij.execution.configurations.RunConfiguration

interface BuildSystemHelperService {

    fun isBuildSystem(externalSystemId: String): Boolean
    fun getEnvironmentMapFromRunConfiguration(config: RunConfiguration): Map<String, String>?
    fun isBuildSystemForConfiguration(config: RunConfiguration): Boolean
    fun updateEnvironmentOnConfiguration(config: RunConfiguration, envs: Map<String, String>)

}
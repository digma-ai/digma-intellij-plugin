package org.digma.intellij.plugin.buildsystem

import com.intellij.execution.configurations.RunConfiguration

interface BuildSystemHelper {

    fun isBuildSystem(externalSystemId: String): Boolean
    fun getBuildSystem(externalSystemId: String): BuildSystem?
    fun isBuildSystemForConfiguration(config: RunConfiguration): Boolean
    fun isMaven(): Boolean
    fun isGradle(): Boolean
    fun getEnvironmentMapFromRunConfiguration(config: RunConfiguration): Map<String, String>?
    fun updateEnvironmentOnConfiguration(config: RunConfiguration, envs: Map<String, String>)

}
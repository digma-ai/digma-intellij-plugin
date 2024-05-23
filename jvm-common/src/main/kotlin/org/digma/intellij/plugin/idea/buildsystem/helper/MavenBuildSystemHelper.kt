package org.digma.intellij.plugin.idea.buildsystem.helper

import com.intellij.execution.configurations.RunConfiguration
import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.buildsystem.BuildSystemHelper
import org.digma.intellij.plugin.buildsystem.BuildSystemHelperService
import org.digma.intellij.plugin.externalsystem.findMavenService

class MavenBuildSystemHelper : BuildSystemHelper {

    private val mavenService: BuildSystemHelperService? = findMavenService()

    override fun isBuildSystem(externalSystemId: String): Boolean {
        return mavenService?.isBuildSystem(externalSystemId) ?: false
    }

    override fun getBuildSystem(externalSystemId: String): BuildSystem? {
        if (isBuildSystem(externalSystemId)) {
            return BuildSystem.MAVEN
        }
        return null
    }

    override fun isBuildSystemForConfiguration(config: RunConfiguration): Boolean {
        return mavenService?.isBuildSystemForConfiguration(config) ?: false
    }

    override fun isMaven(): Boolean {
        return true
    }

    override fun isGradle(): Boolean {
        return false
    }

    override fun getEnvironmentMapFromRunConfiguration(config: RunConfiguration): Map<String, String>? {
        return mavenService?.getEnvironmentMapFromRunConfiguration(config)
    }

    override fun updateEnvironmentOnConfiguration(config: RunConfiguration, envs: Map<String, String>) {
        mavenService?.updateEnvironmentOnConfiguration(config, envs)
    }
}
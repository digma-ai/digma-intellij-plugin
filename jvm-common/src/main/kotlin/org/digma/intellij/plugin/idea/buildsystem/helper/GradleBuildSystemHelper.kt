package org.digma.intellij.plugin.idea.buildsystem.helper

import com.intellij.execution.configurations.RunConfiguration
import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.buildsystem.BuildSystemHelper
import org.digma.intellij.plugin.buildsystem.BuildSystemHelperService
import org.digma.intellij.plugin.externalsystem.findGradleService

class GradleBuildSystemHelper : BuildSystemHelper {

    private val gradleService: BuildSystemHelperService? = findGradleService()

    override fun isBuildSystem(externalSystemId: String): Boolean {
        return gradleService?.isBuildSystem(externalSystemId) ?: false
    }

    override fun getBuildSystem(externalSystemId: String): BuildSystem? {
        if (isBuildSystem(externalSystemId)) {
            return BuildSystem.GRADLE
        }
        return null
    }

    override fun isBuildSystemForConfiguration(config: RunConfiguration): Boolean {
        return gradleService?.isBuildSystemForConfiguration(config) ?: false
    }

    override fun isMaven(): Boolean {
        return false
    }

    override fun isGradle(): Boolean {
        return true
    }

    override fun getEnvironmentMapFromRunConfiguration(config: RunConfiguration): Map<String, String>? {
        return gradleService?.getEnvironmentMapFromRunConfiguration(config)
    }

    override fun updateEnvironmentOnConfiguration(config: RunConfiguration, envs: Map<String, String>) {
        gradleService?.updateEnvironmentOnConfiguration(config, envs)
    }
}
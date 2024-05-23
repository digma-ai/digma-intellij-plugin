package org.digma.intellij.plugin.idea.maven.helper

import com.intellij.execution.configurations.RunConfiguration
import org.digma.intellij.plugin.buildsystem.BuildSystemHelperService
import org.jetbrains.idea.maven.execution.MavenRunConfiguration
import org.jetbrains.idea.maven.execution.MavenRunnerSettings
import org.jetbrains.idea.maven.utils.MavenUtil

//don't change to light service because it will register always. we want it to register only if gradle is enabled.
// see org.digma.intellij-with-gradle.xml
@Suppress("LightServiceMigrationCode")
class MavenService : BuildSystemHelperService {

    override fun isBuildSystem(externalSystemId: String): Boolean {
        return MavenUtil.SYSTEM_ID.id.equals(externalSystemId, true)
    }

    override fun getEnvironmentMapFromRunConfiguration(config: RunConfiguration): Map<String, String>? {
        if (config is MavenRunConfiguration) {
            return config.runnerSettings?.environmentProperties
        }

        return null
    }

    override fun isBuildSystemForConfiguration(config: RunConfiguration): Boolean {
        return config is MavenRunConfiguration
    }

    override fun updateEnvironmentOnConfiguration(config: RunConfiguration, envs: Map<String, String>) {
        if (config is MavenRunConfiguration) {
            val runnerSettings: MavenRunnerSettings =
                if (config.runnerSettings == null) {
                    val runnerSettings = MavenRunnerSettings()
                    config.runnerSettings = runnerSettings
                    runnerSettings
                } else {
                    config.runnerSettings!!
                }


            //replace the map to a mutable map in case it's not
            runnerSettings.environmentProperties = envs
        }
    }

}
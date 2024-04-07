package org.digma.intellij.plugin.idea.maven.execution

import com.intellij.execution.configurations.RunConfiguration
import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.execution.RunConfigurationType
import org.digma.intellij.plugin.idea.execution.services.BaseJvmRunConfigurationInstrumentationService
import org.jetbrains.idea.maven.execution.MavenRunConfiguration


//don't change to light service because it will register always. we want it to register only if gradle is enabled.
// see org.digma.intellij-with-gradle.xml
@Suppress("LightServiceMigrationCode")
class MavenRunConfigurationService : BaseJvmRunConfigurationInstrumentationService() {

    override fun isApplicableFor(configuration: RunConfiguration): Boolean {
        return configuration is MavenRunConfiguration
    }


    override fun getConfigurationType(configuration: RunConfiguration): RunConfigurationType {
        return if (configuration is MavenRunConfiguration) {
            RunConfigurationType.Maven
        } else {
            RunConfigurationType.Unknown
        }
    }

    override fun getTaskNames(configuration: RunConfiguration): Set<String> {
        if (configuration is MavenRunConfiguration) {
            return configuration.runnerParameters.goals.toSet()
        }
        return setOf()
    }

    override fun getBuildSystem(configuration: RunConfiguration): BuildSystem {
        if (configuration is MavenRunConfiguration) {
            return BuildSystem.MAVEN
        }
        return BuildSystem.INTELLIJ
    }


}
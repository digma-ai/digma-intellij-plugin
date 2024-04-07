package org.digma.intellij.plugin.idea.gradle.execution

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.execution.RunConfigurationType
import org.digma.intellij.plugin.idea.execution.ConfigurationCleaner
import org.digma.intellij.plugin.idea.execution.ExternalSystemConfigurationCleaner
import org.digma.intellij.plugin.idea.execution.ExternalSystemJavaToolOptionsMerger
import org.digma.intellij.plugin.idea.execution.JavaToolOptionsMerger
import org.digma.intellij.plugin.idea.execution.ModuleResolver
import org.digma.intellij.plugin.idea.execution.ParametersExtractor
import org.digma.intellij.plugin.idea.execution.services.BaseJvmRunConfigurationInstrumentationService
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration


//don't change to light service because it will register always. we want it to register only if gradle is enabled.
// see org.digma.intellij-with-gradle.xml
@Suppress("LightServiceMigrationCode")
class GradleRunConfigurationService : BaseJvmRunConfigurationInstrumentationService() {

    override fun isApplicableFor(configuration: RunConfiguration): Boolean {
        return configuration is GradleRunConfiguration
    }


    override fun getConfigurationType(configuration: RunConfiguration): RunConfigurationType {
        return if (configuration is GradleRunConfiguration) {
            RunConfigurationType.Gradle
        } else {
            RunConfigurationType.Unknown
        }
    }

    override fun getTaskNames(configuration: RunConfiguration): Set<String> {
        if (configuration is GradleRunConfiguration) {
            return configuration.settings.taskNames.toSet()
        }
        return setOf()
    }

    override fun getBuildSystem(configuration: RunConfiguration): BuildSystem {
        if (configuration is GradleRunConfiguration) {
            return BuildSystem.GRADLE
        }
        return BuildSystem.INTELLIJ
    }

    override fun shouldCleanConfigurationAfterStart(configuration: RunConfiguration): Boolean {
        return true
    }


    override fun getModuleResolver(
        configuration: RunConfiguration,
        params: SimpleProgramParameters,
        runnerSettings: RunnerSettings?
    ): ModuleResolver {
        return GradleModuleResolver(configuration, params)
    }


    override fun getJavaToolOptionsMerger(
        configuration: RunConfiguration,
        params: SimpleProgramParameters,
        parametersExtractor: ParametersExtractor
    ): JavaToolOptionsMerger {
        return ExternalSystemJavaToolOptionsMerger(configuration, params, parametersExtractor)
    }

    override fun getConfigurationCleaner(configuration: RunConfiguration): ConfigurationCleaner {
        return ExternalSystemConfigurationCleaner(configuration)
    }
}
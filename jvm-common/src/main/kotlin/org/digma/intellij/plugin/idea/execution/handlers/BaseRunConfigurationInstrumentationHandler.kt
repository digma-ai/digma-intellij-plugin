package org.digma.intellij.plugin.idea.execution.handlers

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationHandler
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService
import org.digma.intellij.plugin.execution.RunConfigurationType

abstract class BaseRunConfigurationInstrumentationHandler : RunConfigurationInstrumentationHandler {

    abstract fun getService(): RunConfigurationInstrumentationService?


    override fun isApplicableFor(configuration: RunConfiguration): Boolean {
        return getService()?.isApplicableFor(configuration) ?: false
    }

    override fun updateParameters(configuration: RunConfiguration, params: SimpleProgramParameters, runnerSettings: RunnerSettings?): String? {
        return getService()?.updateParameters(configuration, params, runnerSettings)
    }

    override fun getConfigurationDescription(configuration: RunConfiguration, params: SimpleProgramParameters): String {
        return getService()?.getConfigurationDescription(configuration, params) ?: ""
    }

    override fun getConfigurationType(configuration: RunConfiguration): RunConfigurationType {
        return getService()?.getConfigurationType(configuration) ?: RunConfigurationType.Gradle
    }

    override fun shouldCleanConfigurationAfterStart(configuration: RunConfiguration): Boolean {
        return getService()?.shouldCleanConfigurationAfterStart(configuration) ?: false
    }

    override fun cleanConfigurationAfterStart(configuration: RunConfiguration) {
        getService()?.cleanConfigurationAfterStart(configuration)
    }

    override fun getTaskNames(configuration: RunConfiguration): Set<String> {
        return getService()?.getTaskNames(configuration) ?: setOf()
    }

    override fun getBuildSystem(configuration: RunConfiguration): BuildSystem {
        return getService()?.getBuildSystem(configuration) ?: BuildSystem.INTELLIJ
    }
}
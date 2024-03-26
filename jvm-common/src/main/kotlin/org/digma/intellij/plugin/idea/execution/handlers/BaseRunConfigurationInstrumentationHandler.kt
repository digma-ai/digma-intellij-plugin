package org.digma.intellij.plugin.idea.execution.handlers

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationHandler
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService
import org.digma.intellij.plugin.execution.RunConfigurationType

abstract class BaseRunConfigurationInstrumentationHandler : RunConfigurationInstrumentationHandler {

    abstract fun getService(): RunConfigurationInstrumentationService?


    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean {
        return getService()?.isApplicableFor(configuration) ?: false
    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): Boolean {
        return getService()?.isApplicableFor(configuration, params) ?: false
    }

    override fun updateParameters(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters, runnerSettings: RunnerSettings?) {
        getService()?.updateParameters(configuration, params, runnerSettings)
    }

    override fun getConfigurationDescription(configuration: RunConfigurationBase<*>): String {
        return getService()?.getConfigurationDescription(configuration) ?: ""
    }

    override fun getConfigurationType(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): RunConfigurationType {
        return getService()?.getConfigurationType(configuration, params) ?: RunConfigurationType.GradleRun
    }

    override fun shouldCleanConfigurationAfterStart(configuration: RunConfigurationBase<*>): Boolean {
        return getService()?.shouldCleanConfigurationAfterStart(configuration) ?: false
    }

    override fun cleanConfigurationAfterStart(configuration: RunConfigurationBase<*>) {
        getService()?.cleanConfigurationAfterStart(configuration)
    }

    override fun isHandlingType(configuration: RunConfigurationBase<*>): Boolean {
        return getService()?.isHandlingType(configuration) ?: false
    }

    override fun getTaskNames(configuration: RunConfigurationBase<*>): Set<String> {
        return getService()?.getTaskNames(configuration) ?: setOf()
    }

    override fun getBuildSystem(configuration: RunConfigurationBase<*>): BuildSystem {
        return getService()?.getBuildSystem(configuration) ?: BuildSystem.INTELLIJ
    }
}
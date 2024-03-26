package org.digma.intellij.plugin.execution

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.buildsystem.BuildSystem

interface RunConfigurationInstrumentationService {

    /**
     * a not accurate method because there os no params to resolve the module.
     * should be used in combination with other conditions or if matching is not supposed to be accurate.
     */
    fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean

    /**
     * this is the method to check for matching a service to a configuration.
     */
    fun isApplicableFor(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): Boolean

    fun updateParameters(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters, runnerSettings: RunnerSettings?)
    fun getConfigurationDescription(configuration: RunConfigurationBase<*>): String
    fun getConfigurationType(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): RunConfigurationType
    fun shouldCleanConfigurationAfterStart(configuration: RunConfigurationBase<*>): Boolean
    fun cleanConfigurationAfterStart(configuration: RunConfigurationBase<*>)

    /**
     * should be used to check if a specific service can handle this configuration type.
     * should not be used to check is a service can instrument a configuration because there is
     * no params that are used to resolve the module, some services must resolve a module to match.
     */
    fun isHandlingType(configuration: RunConfigurationBase<*>): Boolean
    fun getTaskNames(configuration: RunConfigurationBase<*>): Set<String>
    fun getBuildSystem(configuration: RunConfigurationBase<*>): BuildSystem

}
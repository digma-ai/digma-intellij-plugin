package org.digma.intellij.plugin.execution

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.buildsystem.BuildSystem

interface RunConfigurationInstrumentationService {

    /**
     * not an accurate method because there is no params to resolve the module or environment variables.
     * should be used in combination with other conditions or when matching is not supposed to be accurate.
     * do not use to attach an instrumentation handler to configuration, use fot other needs when
     * the matching doesn't need to be accurate, like for reporting unhandled configuration.
     */
    fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean

    /**
     * this is the method to check for attaching a service to a configuration.
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
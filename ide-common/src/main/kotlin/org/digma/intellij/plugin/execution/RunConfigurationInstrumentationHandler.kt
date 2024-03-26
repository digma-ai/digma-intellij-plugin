package org.digma.intellij.plugin.execution

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.buildsystem.BuildSystem

/**
 * Handles one specific RunConfiguration and adds what ever needed for instrumentation like
 * otel agent , micrometer parameters etc.
 */
interface RunConfigurationInstrumentationHandler {

    /**
     * used to sort handlers. we need it to find a handler that can help with extracting details from configuration.
     * in which case order matters, gradle for example is first, then maven etc.
     */
    fun getOrder(): Int

    /**
     * this method should not be used to find an instrumentation handler, it should only  be used in combination
     * with more conditions. the reason is that there is no params and finding a module is not guaranteed.
     * some instrumentation must have knowledge of the module, for example Quarkus+JavaTest
     */
    fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean

    /**
     * this is the method used to attach an instrumentation handler to configuration. it has all the objects necessary to resolve
     * parameters and the module
     */
    fun isApplicableFor(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): Boolean

    fun updateParameters(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters, runnerSettings: RunnerSettings?)
    fun getConfigurationDescription(configuration: RunConfigurationBase<*>): String
    fun getConfigurationType(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): RunConfigurationType
    fun shouldCleanConfigurationAfterStart(configuration: RunConfigurationBase<*>): Boolean
    fun cleanConfigurationAfterStart(configuration: RunConfigurationBase<*>)

    /**
     * this method should only be used in combination with order, or to find any instrumentation handler that
     * can handle the type. used to find a configuration handler that can help
     * extract details from the configuration for reporting.
     * do not use for attaching instrumentation handler to configuration.
     */
    fun isHandlingType(configuration: RunConfigurationBase<*>): Boolean

    /**
     * this method is relevant only for gradle and maven, others will return empty set.
     */
    fun getTaskNames(configuration: RunConfigurationBase<*>): Set<String>

    fun getBuildSystem(configuration: RunConfigurationBase<*>): BuildSystem

}
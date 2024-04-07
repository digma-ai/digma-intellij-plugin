package org.digma.intellij.plugin.execution

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.buildsystem.BuildSystem

/**
 * Handles one specific RunConfiguration and adds what ever needed for instrumentation like
 * otel agent , micrometer parameters etc.
 */
interface RunConfigurationInstrumentationHandler {

    /**
     * this is the method used to attach an instrumentation handler to configuration
     */
    fun isApplicableFor(configuration: RunConfiguration): Boolean

    /**
     * updates the configuration with instrumentation parameters.
     * returns the java tool options for reporting, or null, or error message.
     */
    fun updateParameters(configuration: RunConfiguration, params: SimpleProgramParameters, runnerSettings: RunnerSettings?): String?

    /**
     * returns a description for logging
     */
    fun getConfigurationDescription(configuration: RunConfiguration, params: SimpleProgramParameters): String

    /**
     * returns a configuration type. this is an internal type that we use for logging only
     */
    fun getConfigurationType(configuration: RunConfiguration): RunConfigurationType

    fun shouldCleanConfigurationAfterStart(configuration: RunConfiguration): Boolean

    fun cleanConfigurationAfterStart(configuration: RunConfiguration)

    /**
     * this method is relevant only for gradle and maven, others will return empty set.
     */
    fun getTaskNames(configuration: RunConfiguration): Set<String>

    fun getBuildSystem(configuration: RunConfiguration): BuildSystem

}
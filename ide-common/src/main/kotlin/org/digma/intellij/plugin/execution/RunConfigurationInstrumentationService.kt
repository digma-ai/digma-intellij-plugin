package org.digma.intellij.plugin.execution

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.buildsystem.BuildSystem

const val DIGMA_INSTRUMENTATION_ERROR = "DIGMA_INSTRUMENTATION_ERROR"

interface RunConfigurationInstrumentationService {

    /**
     * this method decides if this service handles the configuration
     */
    fun isApplicableFor(configuration: RunConfiguration): Boolean

    /**
     * updates the configuration with instrumentation parameters.
     * returns the java tool options for reporting, or null if not handled.
     * the configuration may not be updated because we don't support everything. the service may decide not to instrument.
     * may return a string starting with DIGMA_INSTRUMENTATION_ERROR for reporting in case of error.
     */
    fun updateParameters(configuration: RunConfiguration, params: SimpleProgramParameters, runnerSettings: RunnerSettings?): String?

    /**
     * returns a description for logging
     */
    fun getConfigurationDescription(configuration: RunConfiguration, params: SimpleProgramParameters): String

    fun getConfigurationType(configuration: RunConfiguration): RunConfigurationType

    fun shouldCleanConfigurationAfterStart(configuration: RunConfiguration): Boolean
    fun cleanConfigurationAfterStart(configuration: RunConfiguration)

    /**
     * this method is relevant only for gradle and maven, others will return empty set.
     */
    fun getTaskNames(configuration: RunConfiguration): Set<String>

    fun getBuildSystem(configuration: RunConfiguration): BuildSystem

}
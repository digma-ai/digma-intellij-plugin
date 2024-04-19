package org.digma.intellij.plugin.idea.execution.flavor

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService
import org.digma.intellij.plugin.idea.execution.ModuleResolver
import org.digma.intellij.plugin.idea.execution.ParametersExtractor
import org.digma.intellij.plugin.idea.execution.ProjectHeuristics


/**
 * base instrumentation flavor.
 */
abstract class BaseInstrumentationFlavor : InstrumentationFlavor {


    abstract fun accept(
        instrumentationService: RunConfigurationInstrumentationService,
        configuration: RunConfiguration,
        params: SimpleProgramParameters,
        runnerSettings: RunnerSettings?,
        projectHeuristics: ProjectHeuristics,
        moduleResolver: ModuleResolver,
        parametersExtractor: ParametersExtractor
    ): Boolean


    final override fun accept(
        userInstrumentationFlavorType: InstrumentationFlavorType?,
        instrumentationService: RunConfigurationInstrumentationService,
        configuration: RunConfiguration,
        params: SimpleProgramParameters,
        runnerSettings: RunnerSettings?,
        projectHeuristics: ProjectHeuristics,
        moduleResolver: ModuleResolver,
        parametersExtractor: ParametersExtractor
    ): Boolean {

        //if user configured flavor, it wins, there is nothing more to check
        if (userInstrumentationFlavorType != null) {
            return userInstrumentationFlavorType == getFlavor()
        }

        return accept(instrumentationService, configuration, params, runnerSettings, projectHeuristics, moduleResolver, parametersExtractor)
    }


    open fun useOtelAgent(): Boolean {
        return true
    }

    open fun isTest(
        instrumentationService: RunConfigurationInstrumentationService,
        configuration: RunConfiguration,
        params: SimpleProgramParameters
    ): Boolean {
        return false
    }


}
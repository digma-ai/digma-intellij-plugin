package org.digma.intellij.plugin.idea.execution.flavor

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService
import org.digma.intellij.plugin.idea.execution.ModuleResolver
import org.digma.intellij.plugin.idea.execution.ParametersExtractor
import org.digma.intellij.plugin.idea.execution.ProjectHeuristics
import org.digma.intellij.plugin.idea.execution.services.JavaServerRunConfigurationInstrumentationService

//JavaServerInstrumentationFlavor is exactly like default except it is never test.
class JavaServerInstrumentationFlavor : DefaultInstrumentationFlavor() {

    //it is higher order then default
    override fun getOrder(): Int {
        return 10
    }

    override fun getPreferredUserFlavor(): Flavor {
        return Flavor.JavaServer
    }

    override fun accept(
        instrumentationService: RunConfigurationInstrumentationService,
        configuration: RunConfiguration,
        params: SimpleProgramParameters,
        runnerSettings: RunnerSettings?,
        projectHeuristics: ProjectHeuristics,
        moduleResolver: ModuleResolver,
        parametersExtractor: ParametersExtractor
    ): Boolean {
        return instrumentationService is JavaServerRunConfigurationInstrumentationService
    }

    override fun isTest(
        instrumentationService: RunConfigurationInstrumentationService,
        configuration: RunConfiguration,
        params: SimpleProgramParameters
    ): Boolean {
        return false
    }
}
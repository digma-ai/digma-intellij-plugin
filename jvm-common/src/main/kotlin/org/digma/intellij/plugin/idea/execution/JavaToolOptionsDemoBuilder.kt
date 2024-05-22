package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.application.ApplicationConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.errorreporting.ErrorReporter

class JavaToolOptionsDemoBuilder {

    fun isSupported(configuration: RunConfiguration): Boolean {
        return RunConfigurationHandlersHolder.runConfigurationHandlers.find { it.isApplicableFor(configuration) } != null
    }


    fun buildDemoJavaToolOptions(configuration: RunConfiguration): String? {

        //we want to build java tool options. instead of refactoring all related classes we can use a dummy
        // configuration and run the RunConfigurationInstrumentationHandler on it. fortunately the method
        // updateParameters returns the java tool options, and we can use it.
        // using ApplicationConfiguration makes sure its supported.

        try {
            val factory = ApplicationConfigurationType.getInstance().configurationFactories.first()
            val template = factory.createTemplateConfiguration(configuration.project)
            val dummyConfig = factory.createConfiguration("dummy", template)

            val handler = RunConfigurationHandlersHolder.runConfigurationHandlers.find { it.isApplicableFor(dummyConfig) }

            if (handler == null) {
                return null
            }


            val instrumentationParams = handler.updateParameters(dummyConfig, SimpleProgramParameters(), null)
            return instrumentationParams?.first
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("SetRunConfigurationMessageBuilder.sendNotSupportedWithJavaToolOptions", e)
            return null
        }
    }

}
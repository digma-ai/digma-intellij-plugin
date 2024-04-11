package org.digma.intellij.plugin.idea.execution.services

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.execution.jar.JarApplicationConfiguration
import com.intellij.openapi.module.Module
import org.digma.intellij.plugin.execution.RunConfigurationType
import org.digma.intellij.plugin.idea.execution.ModuleResolver

//don't change to light service because it will register always. we want it to register only for Idea
@Suppress("LightServiceMigrationCode")
class JarRunConfigurationInstrumentationService : BaseJvmRunConfigurationInstrumentationService() {

    override fun isApplicableFor(configuration: RunConfiguration): Boolean {
        return configuration is JarApplicationConfiguration
    }

    override fun getConfigurationType(configuration: RunConfiguration): RunConfigurationType {
        return if (isApplicableFor(configuration)) {
            RunConfigurationType.Jar
        } else {
            RunConfigurationType.Unknown
        }
    }

    override fun getModuleResolver(
        configuration: RunConfiguration,
        params: SimpleProgramParameters,
        runnerSettings: RunnerSettings?
    ): ModuleResolver {
        return JarConfigurationModuleResolver(configuration, params)
    }

}


private class JarConfigurationModuleResolver(configuration: RunConfiguration, params: SimpleProgramParameters) :
    ModuleResolver(configuration, params) {

    override fun resolveModule(): Module? {
        if (configuration is JarApplicationConfiguration && configuration.module != null) {
            return configuration.module
        }
        return super.resolveModule()
    }
}
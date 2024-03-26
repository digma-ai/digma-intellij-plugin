package org.digma.intellij.plugin.idea.execution.services

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.execution.jar.JarApplicationConfiguration
import com.intellij.openapi.module.Module
import org.digma.intellij.plugin.execution.RunConfigurationType
import org.digma.intellij.plugin.idea.execution.ModuleResolver

//don't change to light service because it will register always. we want it to register only for Idea
@Suppress("LightServiceMigrationCode")
class JarRunConfigurationInstrumentationService : BaseJvmRunConfigurationInstrumentationService() {

    private fun isHandlingConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        return configuration is JarApplicationConfiguration
    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean {
        return isHandlingConfiguration(configuration)
    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): Boolean {
        return isHandlingConfiguration(configuration)
    }


    override fun getConfigurationType(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): RunConfigurationType {
        if (isHandlingConfiguration(configuration)) {
            return RunConfigurationType.Jar
        }
        return RunConfigurationType.Unknown
    }

    override fun isHandlingType(configuration: RunConfigurationBase<*>): Boolean {
        return isHandlingConfiguration(configuration)
    }


    override fun getModuleResolver(
        configuration: RunConfigurationBase<*>,
        params: SimpleProgramParameters,
        runnerSettings: RunnerSettings?
    ): ModuleResolver {
        return JarConfigurationModuleResolver(configuration, params)
    }

}


private class JarConfigurationModuleResolver(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters) :
    ModuleResolver(configuration, params) {

    override fun resolveModule(): Module? {
        if (configuration is JarApplicationConfiguration && configuration.module != null) {
            return configuration.module
        }
        return super.resolveModule()
    }
}
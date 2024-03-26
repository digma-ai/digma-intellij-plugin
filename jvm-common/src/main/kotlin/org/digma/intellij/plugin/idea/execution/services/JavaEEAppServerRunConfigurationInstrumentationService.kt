package org.digma.intellij.plugin.idea.execution.services

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.openapi.module.Module
import org.digma.intellij.plugin.execution.RunConfigurationType

//don't change to light service because it will register always. we want it to register only for Idea
@Suppress("LightServiceMigrationCode")
class JavaEEAppServerRunConfigurationInstrumentationService : BaseJvmRunConfigurationInstrumentationService() {

    private val supportedEeAppServerTypeIds: Set<String> = setOf(
        "JBossConfiguration", // also WildFly
        "GlassfishConfiguration",
        "TomeeConfiguration",
    )


    private fun isHandlingConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        return supportedEeAppServerTypeIds.contains(configuration.type.id)
    }


    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean {
        return isHandlingConfiguration(configuration)
    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): Boolean {
        return isHandlingConfiguration(configuration)
    }


    override fun getConfigurationType(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): RunConfigurationType {
        if (isHandlingConfiguration(configuration)) {
            return RunConfigurationType.EeAppSeverAtIdeaUltimate
        }
        return RunConfigurationType.Unknown
    }

    override fun isHandlingType(configuration: RunConfigurationBase<*>): Boolean {
        return isHandlingConfiguration(configuration)
    }

    override fun shouldUseOtelAgent(resolvedModule: Module?): Boolean {
        return true
    }

    override fun isSpringBootMicrometerTracing(module: Module?): Boolean {
        return false
    }

    override fun isMicronautModule(module: Module?): Boolean {
        return false
    }

}
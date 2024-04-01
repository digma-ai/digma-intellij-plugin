package org.digma.intellij.plugin.idea.execution.services

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.execution.RunConfigurationType
import org.digma.intellij.plugin.idea.execution.InstrumentationFlavor
import org.digma.intellij.plugin.idea.execution.ModuleResolver
import org.digma.intellij.plugin.idea.execution.ParametersExtractor
import org.digma.intellij.plugin.idea.execution.ProjectHeuristics

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


    override fun getInstrumentationFlavor(
        configuration: RunConfigurationBase<*>,
        projectHeuristics: ProjectHeuristics,
        moduleResolver: ModuleResolver,
        parametersExtractor: ParametersExtractor
    ): InstrumentationFlavor {
        return JavaEEInstrumentationFlavor(configuration, projectHeuristics, moduleResolver, parametersExtractor)
    }

    private class JavaEEInstrumentationFlavor(
        configuration: RunConfigurationBase<*>,
        projectHeuristics: ProjectHeuristics,
        moduleResolver: ModuleResolver,
        parametersExtractor: ParametersExtractor
    ) : InstrumentationFlavor(configuration, projectHeuristics, moduleResolver, parametersExtractor) {

        override fun shouldUseOtelAgent(): Boolean {
            return true
        }

        override fun isSpringBootMicrometerTracing(): Boolean {
            return false
        }

        override fun isMicronautTracing(): Boolean {
            return false
        }
    }

}
package org.digma.intellij.plugin.idea.execution.services

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService
import org.digma.intellij.plugin.idea.execution.ConfigurationCleaner
import org.digma.intellij.plugin.idea.execution.InstrumentationFlavor
import org.digma.intellij.plugin.idea.execution.JavaToolOptionsBuilder
import org.digma.intellij.plugin.idea.execution.JavaToolOptionsMerger
import org.digma.intellij.plugin.idea.execution.ModuleResolver
import org.digma.intellij.plugin.idea.execution.ParametersExtractor
import org.digma.intellij.plugin.idea.execution.ProjectHeuristics
import org.digma.intellij.plugin.idea.execution.ServiceNameProvider

/**
 * base class that covers most of the configuration types.
 */
abstract class BaseJvmRunConfigurationInstrumentationService : RunConfigurationInstrumentationService {

    override fun updateParameters(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters, runnerSettings: RunnerSettings?) {

        val projectHeuristics = getProjectHeuristics(configuration.project)
        val moduleResolver = getModuleResolver(configuration, params)
        val parametersExtractor = getParametersExtractor(configuration, params)

        val instrumentationFlavor = getInstrumentationFlavor(
            configuration,
            projectHeuristics,
            moduleResolver,
            parametersExtractor
        )

        val serviceNameProvider = getServiceNameProvider(configuration, params)

        val javaToolOptionsBuilder = getJavaToolOptionsBuilder(configuration, params, parametersExtractor, serviceNameProvider)

        val javaToolOptions: String? = try {
            javaToolOptionsBuilder
                .withOtelAgent(instrumentationFlavor.shouldUseOtelAgent())
                .withSpringBootWithMicrometerTracing(instrumentationFlavor.isSpringBootMicrometerTracing())
                .withMicronautTracing(instrumentationFlavor.isMicronautTracing())
                .withTest(isTest(configuration, params))
                .withResourceAttributes(isTest(configuration, params))
                .withServiceName(moduleResolver)
                .withExtendedObservability()
                .withOtelDebug()
                .withCommonProperties()
                .build()
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("${this::class.java}.updateParameters", e)
            null
        }


        javaToolOptions?.let {
            getJavaToolOptionsMerger(configuration, params, parametersExtractor).mergeJavaToolOptions(javaToolOptions)
        }

    }


    open fun getInstrumentationFlavor(
        configuration: RunConfigurationBase<*>,
        projectHeuristics: ProjectHeuristics,
        moduleResolver: ModuleResolver,
        parametersExtractor: ParametersExtractor
    ): InstrumentationFlavor {
        return InstrumentationFlavor(configuration, projectHeuristics, moduleResolver, parametersExtractor)
    }


    open fun getProjectHeuristics(project: Project): ProjectHeuristics {
        return ProjectHeuristics(project)
    }

    open fun getModuleResolver(
        configuration: RunConfigurationBase<*>,
        params: SimpleProgramParameters,
        runnerSettings: RunnerSettings? = null
    ): ModuleResolver {
        return ModuleResolver(configuration, params)
    }

    open fun getParametersExtractor(
        configuration: RunConfigurationBase<*>,
        params: SimpleProgramParameters,
        runnerSettings: RunnerSettings? = null
    ): ParametersExtractor {
        return ParametersExtractor(configuration, params)
    }

    open fun getServiceNameProvider(
        configuration: RunConfigurationBase<*>,
        params: SimpleProgramParameters,
        runnerSettings: RunnerSettings? = null
    ): ServiceNameProvider {
        return ServiceNameProvider(configuration, params)
    }

    open fun getJavaToolOptionsBuilder(
        configuration: RunConfigurationBase<*>,
        params: SimpleProgramParameters,
        parametersExtractor: ParametersExtractor,
        serviceNameProvider: ServiceNameProvider,
        runnerSettings: RunnerSettings? = null
    ): JavaToolOptionsBuilder {
        return JavaToolOptionsBuilder(configuration, params, parametersExtractor, serviceNameProvider)
    }


    open fun getJavaToolOptionsMerger(
        configuration: RunConfigurationBase<*>,
        params: SimpleProgramParameters,
        parametersExtractor: ParametersExtractor
    ): JavaToolOptionsMerger {
        return JavaToolOptionsMerger(configuration, params, parametersExtractor)
    }

    open fun getConfigurationCleaner(
        configuration: RunConfigurationBase<*>
    ): ConfigurationCleaner {
        return ConfigurationCleaner(configuration)
    }


    open fun isTest(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters?): Boolean {
        return false
    }

    override fun getConfigurationDescription(configuration: RunConfigurationBase<*>): String {
        val taskNames = getTaskNames(configuration)
        if (taskNames.isEmpty()) {
            return configuration.toString()
        }
        return configuration.toString() + ",tasks=${taskNames.joinToString()}"
    }

    override fun shouldCleanConfigurationAfterStart(configuration: RunConfigurationBase<*>): Boolean {
        return false
    }

    override fun cleanConfigurationAfterStart(configuration: RunConfigurationBase<*>) {
        //the default ConfigurationCleaner does nothing
        getConfigurationCleaner(configuration).cleanConfiguration()
    }

    override fun getTaskNames(configuration: RunConfigurationBase<*>): Set<String> {
        if (configuration is ExternalSystemRunConfiguration) {
            return configuration.settings.taskNames.toSet()
        }
        return setOf()
    }

    override fun getBuildSystem(configuration: RunConfigurationBase<*>): BuildSystem {
        return BuildSystem.INTELLIJ
    }
}
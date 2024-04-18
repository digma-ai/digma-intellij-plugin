package org.digma.intellij.plugin.idea.execution.services

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.execution.DIGMA_INSTRUMENTATION_ERROR
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService
import org.digma.intellij.plugin.idea.execution.ConfigurationCleaner
import org.digma.intellij.plugin.idea.execution.JavaParametersMerger
import org.digma.intellij.plugin.idea.execution.JavaToolOptionsBuilder
import org.digma.intellij.plugin.idea.execution.ModuleResolver
import org.digma.intellij.plugin.idea.execution.OtelResourceAttributesBuilder
import org.digma.intellij.plugin.idea.execution.ParametersExtractor
import org.digma.intellij.plugin.idea.execution.ProjectHeuristics
import org.digma.intellij.plugin.idea.execution.ServiceNameProvider
import org.digma.intellij.plugin.idea.execution.flavor.InstrumentationFlavor

/**
 * base class that covers most of the configuration types.
 */
abstract class BaseJvmRunConfigurationInstrumentationService : RunConfigurationInstrumentationService {

    override fun updateParameters(configuration: RunConfiguration, params: SimpleProgramParameters, runnerSettings: RunnerSettings?): String? {

        val projectHeuristics = getProjectHeuristics(configuration.project)
        val moduleResolver = getModuleResolver(configuration, params)
        val parametersExtractor = getParametersExtractor(configuration, params)
        val serviceNameProvider = getServiceNameProvider(configuration, params)
        val javaToolOptionsBuilder = getJavaToolOptionsBuilder(configuration, params, runnerSettings)
        val otelResourceAttributesBuilder = getOtelResourceAttributesBuilder(configuration, params, runnerSettings)


        val instrumentationFlavor: InstrumentationFlavor? =
            InstrumentationFlavor.get(
                this,
                configuration,
                params,
                runnerSettings,
                projectHeuristics,
                moduleResolver,
                parametersExtractor
            )

        if (instrumentationFlavor == null) {
            return null
        }


        return try {
            var javaToolOptions = instrumentationFlavor.buildJavaToolOptions(
                this,
                javaToolOptionsBuilder,
                configuration,
                params,
                runnerSettings,
                projectHeuristics,
                moduleResolver,
                parametersExtractor,
                serviceNameProvider
            )

            javaToolOptions = javaToolOptions?.let {
                //easier for us to see which flavor
                " -Ddigma.flavor=${instrumentationFlavor.getFlavor()} ".plus(it)
            }

            val otelResourceAttributes = instrumentationFlavor.buildOtelResourceAttributes(
                this,
                otelResourceAttributesBuilder,
                configuration,
                params,
                projectHeuristics,
                moduleResolver,
                parametersExtractor,
            )


            getJavaParametersMerger(configuration, params, parametersExtractor)
                .mergeJavaToolOptionsAndOtelResourceAttributes(instrumentationFlavor.getFlavor(), javaToolOptions, otelResourceAttributes)

            return javaToolOptions

        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("${this::class.java}.updateParameters", e)
            "$DIGMA_INSTRUMENTATION_ERROR $e"
        }

    }


    open fun getProjectHeuristics(project: Project): ProjectHeuristics {
        return ProjectHeuristics(project)
    }

    open fun getModuleResolver(
        configuration: RunConfiguration,
        params: SimpleProgramParameters,
        runnerSettings: RunnerSettings? = null
    ): ModuleResolver {
        return ModuleResolver(configuration, params)
    }

    open fun getParametersExtractor(
        configuration: RunConfiguration,
        params: SimpleProgramParameters,
        runnerSettings: RunnerSettings? = null
    ): ParametersExtractor {
        return ParametersExtractor(configuration, params)
    }

    open fun getServiceNameProvider(
        configuration: RunConfiguration,
        params: SimpleProgramParameters,
        runnerSettings: RunnerSettings? = null
    ): ServiceNameProvider {
        return ServiceNameProvider(configuration, params)
    }

    open fun getJavaToolOptionsBuilder(
        configuration: RunConfiguration,
        params: SimpleProgramParameters,
        runnerSettings: RunnerSettings? = null
    ): JavaToolOptionsBuilder {
        return JavaToolOptionsBuilder(configuration, params, runnerSettings)
    }

    open fun getOtelResourceAttributesBuilder(
        configuration: RunConfiguration,
        params: SimpleProgramParameters,
        runnerSettings: RunnerSettings? = null
    ): OtelResourceAttributesBuilder {
        return OtelResourceAttributesBuilder(configuration, params, runnerSettings)
    }


    open fun getJavaParametersMerger(
        configuration: RunConfiguration,
        params: SimpleProgramParameters,
        parametersExtractor: ParametersExtractor
    ): JavaParametersMerger {
        return JavaParametersMerger(configuration, params, parametersExtractor)
    }

    open fun getConfigurationCleaner(
        configuration: RunConfiguration
    ): ConfigurationCleaner {
        return ConfigurationCleaner(configuration)
    }


    override fun getConfigurationDescription(configuration: RunConfiguration, params: SimpleProgramParameters): String {
        var desc = configuration.toString() + ",type id:${configuration.type.id}"
        val taskNames = getTaskNames(configuration)
        if (taskNames.isNotEmpty()) {
            desc = desc.plus(",tasks=${taskNames.joinToString()}")
        }
        return desc
    }

    override fun shouldCleanConfigurationAfterStart(configuration: RunConfiguration): Boolean {
        return false
    }

    override fun cleanConfigurationAfterStart(configuration: RunConfiguration) {
        //the default ConfigurationCleaner does nothing
        getConfigurationCleaner(configuration).cleanConfiguration()
    }

    override fun getTaskNames(configuration: RunConfiguration): Set<String> {
        if (configuration is ExternalSystemRunConfiguration) {
            return configuration.settings.taskNames.toSet()
        }
        return setOf()
    }

    override fun getBuildSystem(configuration: RunConfiguration): BuildSystem {
        return BuildSystem.INTELLIJ
    }
}
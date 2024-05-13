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
import org.digma.intellij.plugin.idea.execution.OTEL_RESOURCE_ATTRIBUTES
import org.digma.intellij.plugin.idea.execution.OtelResourceAttributesBuilder
import org.digma.intellij.plugin.idea.execution.ParametersExtractor
import org.digma.intellij.plugin.idea.execution.ProjectHeuristics
import org.digma.intellij.plugin.idea.execution.ServiceNameProvider
import org.digma.intellij.plugin.idea.execution.flavor.InstrumentationFlavor
import org.digma.intellij.plugin.idea.execution.flavor.InstrumentationFlavorType

/**
 * base class that covers most of the configuration types.
 */
abstract class BaseJvmRunConfigurationInstrumentationService : RunConfigurationInstrumentationService {

    override fun updateParameters(
        configuration: RunConfiguration,
        params: SimpleProgramParameters,
        runnerSettings: RunnerSettings?
    ): Pair<String, String>? {

        val projectHeuristics = getProjectHeuristics(configuration.project)
        val moduleResolver = getModuleResolver(configuration, params)
        val parametersExtractor = getParametersExtractor(configuration, params)
        val serviceNameProvider = getServiceNameProvider(configuration, params)
        val javaToolOptionsBuilder = getJavaToolOptionsBuilder(configuration, params, runnerSettings)
        val otelResourceAttributesBuilder = getOtelResourceAttributesBuilder(configuration, params, runnerSettings)


        var instrumentationFlavor: InstrumentationFlavor? =
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

            //if no instrumentationFlavor was selected then maybe it's a gradle or maven task that we don't support.
            //if it's not gradle or maven return null. otherwise check if DIGMA_OBSERVABILITY exists to force observability.
            val buildSystem = getBuildSystem(configuration)
            if (buildSystem != BuildSystem.GRADLE && buildSystem != BuildSystem.MAVEN) {
                return null
            }

            //check is DIGMA_OBSERVABILITY exists else return null.
            //DIGMA_OBSERVABILITY is also used to decide if to force app or test in the various flavors.
            parametersExtractor.getDigmaObservability()
                ?: return null

            //if DIGMA_OBSERVABILITY exists force observability with InstrumentationFlavorType.Default.
            //no need to check if INSTRUMENTATION_FLAVOR exists, if it did InstrumentationFlavor.get would return something.
            //InstrumentationFlavor.getByType must return something, or we have a bug. it will throw an exception
            // if not. the exception will be caught and reported.
            instrumentationFlavor = InstrumentationFlavor.getByType(InstrumentationFlavorType.Default)

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
                //easier for us to see which flavor was selected
                var newValue = " -Ddigma.flavor=${instrumentationFlavor.getFlavor()} ".plus(it)

                //and if DIGMA_OBSERVABILITY was configured
                if (parametersExtractor.getDigmaObservability() != null) {
                    newValue = " -Ddigma.observability=${parametersExtractor.getDigmaObservability()} $newValue"
                }

                newValue

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


            //we need this return value only for reporting what was added to the configuration
            val otelResourceAttributesEnv = parametersExtractor.extractEnvValue(OTEL_RESOURCE_ATTRIBUTES)
            return Pair(javaToolOptions.toString(), otelResourceAttributesEnv.toString())

        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("${this::class.java}.updateParameters", e)
            Pair("$DIGMA_INSTRUMENTATION_ERROR $e", "")
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
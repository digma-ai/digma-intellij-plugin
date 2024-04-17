package org.digma.intellij.plugin.idea.execution.flavor

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.openapi.module.Module
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService
import org.digma.intellij.plugin.idea.execution.JavaToolOptionsBuilder
import org.digma.intellij.plugin.idea.execution.ModuleResolver
import org.digma.intellij.plugin.idea.execution.OtelResourceAttributesBuilder
import org.digma.intellij.plugin.idea.execution.ParametersExtractor
import org.digma.intellij.plugin.idea.execution.ProjectHeuristics
import org.digma.intellij.plugin.idea.execution.ServiceNameProvider
import org.digma.intellij.plugin.idea.execution.getModuleMetadata
import org.digma.intellij.plugin.idea.execution.isMicrometerTracingInSettings


//SpringBootMicrometerInstrumentationFlavor supports the same gradle/maven tasks as DefaultInstrumentationFlavor.
class SpringBootMicrometerInstrumentationFlavor : DefaultInstrumentationFlavor() {


    override fun getOrder(): Int {
        return 1
    }

    override fun getPreferredUserFlavor(): Flavor {
        return Flavor.SpringBootMicrometer
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

        //first check super, this flavor supports the same gradle/maven tasks as default
        val applicable =
            super.accept(instrumentationService, configuration, params, runnerSettings, projectHeuristics, moduleResolver, parametersExtractor)

        if (!applicable) {
            return false
        }

        val module = moduleResolver.resolveModule()
        return isSpringBootMicrometerTracing(module, projectHeuristics)
    }


    private fun isSpringBootMicrometerTracing(module: Module?, projectHeuristics: ProjectHeuristics): Boolean {
        val isMicrometerTracingInSettings = isMicrometerTracingInSettings()
        val isSpringBootModule = isSpringBootModule(module)
        return isMicrometerTracingInSettings &&
                (isSpringBootModule || projectHeuristics.hasOnlySpringBootModules())
    }

    private fun isSpringBootModule(module: Module?): Boolean {
        val moduleMetadata = getModuleMetadata(module)
        return moduleMetadata?.hasSpringBoot() ?: false
    }


    override fun buildJavaToolOptions(
        instrumentationService: RunConfigurationInstrumentationService,
        javaToolOptionsBuilder: JavaToolOptionsBuilder,
        configuration: RunConfiguration,
        params: SimpleProgramParameters,
        runnerSettings: RunnerSettings?,
        projectHeuristics: ProjectHeuristics,
        moduleResolver: ModuleResolver,
        parametersExtractor: ParametersExtractor,
        serviceNameProvider: ServiceNameProvider
    ): String? {
        return try {

            val isTest = isTest(instrumentationService, configuration, params)

            javaToolOptionsBuilder
                .withSpringBootWithMicrometerTracing(true)
                .withMockitoSupport(isTest)
                .withServiceName(moduleResolver, parametersExtractor, serviceNameProvider)
                .withExtendedObservability()
                .withOtelDebug()
                .withCommonProperties()
                .build()

        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("${this::class.java}.buildJavaToolOptions", e)
            null
        }
    }


    override fun buildOtelResourceAttributes(
        instrumentationService: RunConfigurationInstrumentationService,
        otelResourceAttributesBuilder: OtelResourceAttributesBuilder,
        configuration: RunConfiguration,
        params: SimpleProgramParameters,
        projectHeuristics: ProjectHeuristics,
        moduleResolver: ModuleResolver,
        parametersExtractor: ParametersExtractor
    ): String? {

        return try {
            val isTest = isTest(instrumentationService, configuration, params)

            otelResourceAttributesBuilder
                .withCommonResourceAttributes(isTest, parametersExtractor)
                .build()

        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("${this::class.java}.buildJavaToolOptions", e)
            null
        }
    }


    //actually not relevant because this flavor never calls withOtelAgent
    override fun useOtelAgent(): Boolean {
        return false
    }

}
package org.digma.intellij.plugin.idea.execution.flavor

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.openapi.module.Module
import org.digma.intellij.plugin.analytics.LOCAL_ENV
import org.digma.intellij.plugin.analytics.LOCAL_TESTS_ENV
import org.digma.intellij.plugin.analytics.isCentralized
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
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
import org.digma.intellij.plugin.vcs.VcsService

private const val ATTRIBUTES_PREFIX = "MANAGEMENT_OPENTELEMETRY_RESOURCE-ATTRIBUTES"
private const val ENVIRONMENT_RESOURCE_ATTRIBUTE = "${ATTRIBUTES_PREFIX}_digma_environment"
private const val ENVIRONMENT_ID_RESOURCE_ATTRIBUTE = "${ATTRIBUTES_PREFIX}_digma_environment_id"
private const val USER_ID_RESOURCE_ATTRIBUTE = "${ATTRIBUTES_PREFIX}_digma_user_id"
private const val SCM_COMMIT_ID_RESOURCE_ATTRIBUTE = "${ATTRIBUTES_PREFIX}_scm_commit_id"

//SpringBootMicrometerInstrumentationFlavor supports the same gradle/maven tasks as DefaultInstrumentationFlavor.
class SpringBootMicrometerInstrumentationFlavor : DefaultInstrumentationFlavor() {

    companion object {
        fun getEnvironmentIdAttributeKey(): String {
            return ENVIRONMENT_ID_RESOURCE_ATTRIBUTE
        }
    }


    override fun getOrder(): Int {
        return 1
    }

    override fun getFlavor(): InstrumentationFlavorType {
        return InstrumentationFlavorType.SpringBootMicrometer
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
                .withCommonSpringBootWithMicrometerTracing(true)
                .withOtelTracesExporterOtlp()
                .withOtelMetricsExporterNone()
                .withOtelExperimentalSpanSuppressionStrategyNone()
                .withServiceName(moduleResolver, parametersExtractor, serviceNameProvider)
                .withMockitoSupport(isTest)
                .withExtendedObservability()
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
    ): Map<String, String> {

        return try {
            val isTest = isTest(instrumentationService, configuration, params)

            if (needToAddDigmaEnvironmentAttribute(parametersExtractor)) {
                if (isTest) {
                    otelResourceAttributesBuilder.withOtelResourceAttribute(ENVIRONMENT_RESOURCE_ATTRIBUTE, LOCAL_TESTS_ENV)
                } else {
                    otelResourceAttributesBuilder.withOtelResourceAttribute(ENVIRONMENT_RESOURCE_ATTRIBUTE, LOCAL_ENV)
                }

            }

            if (!hasEnvironmentIdAttribute(parametersExtractor) &&
                isCentralized(configuration.project)
            ) {
                DigmaDefaultAccountHolder.getInstance().account?.userId?.let {
                    otelResourceAttributesBuilder.withOtelResourceAttribute(USER_ID_RESOURCE_ATTRIBUTE, it)
                }
            }

            VcsService.getInstance(configuration.project).getCommitIdForCurrentProject()?.let {
                otelResourceAttributesBuilder.withOtelResourceAttribute(SCM_COMMIT_ID_RESOURCE_ATTRIBUTE, it)
            }

            otelResourceAttributesBuilder
                .build()

        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("${this::class.java}.buildJavaToolOptions", e)
            mapOf()
        }
    }


    private fun needToAddDigmaEnvironmentAttribute(parametersExtractor: ParametersExtractor): Boolean {
        return !hasEnvironmentAttribute(parametersExtractor) && !hasEnvironmentIdAttribute(parametersExtractor)
    }

    private fun hasEnvironmentAttribute(parametersExtractor: ParametersExtractor): Boolean {
        return parametersExtractor.extractEnvValue(ENVIRONMENT_RESOURCE_ATTRIBUTE) != null
    }

    private fun hasEnvironmentIdAttribute(parametersExtractor: ParametersExtractor): Boolean {
        return parametersExtractor.extractEnvValue(ENVIRONMENT_ID_RESOURCE_ATTRIBUTE) != null
    }


    //actually not relevant because this flavor never calls withOtelAgent
    override fun useOtelAgent(): Boolean {
        return false
    }

}
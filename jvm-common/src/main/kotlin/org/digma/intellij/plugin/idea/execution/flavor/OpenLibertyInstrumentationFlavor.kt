package org.digma.intellij.plugin.idea.execution.flavor

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService
import org.digma.intellij.plugin.idea.execution.JavaToolOptionsBuilder
import org.digma.intellij.plugin.idea.execution.ModuleResolver
import org.digma.intellij.plugin.idea.execution.OtelResourceAttributesBuilder
import org.digma.intellij.plugin.idea.execution.ParametersExtractor
import org.digma.intellij.plugin.idea.execution.ProjectHeuristics
import org.digma.intellij.plugin.idea.execution.ServiceNameProvider

class OpenLibertyInstrumentationFlavor : BaseInstrumentationFlavor() {

    companion object {
        private val SUPPORTED_GRADLE_TASKS = listOf("libertyDev", "libertyRun", "libertyStart")
        private val SUPPORTED_GRADLE_TEST_TASKS = listOf<String>() // currently no special task for liberty test
        private val SUPPORTED_MAVEN_GOALS = listOf(
            "liberty:dev",
            "liberty-maven-plugin:~:dev",
            "liberty:run",
            "liberty-maven-plugin:~:run",
            "liberty:start",
            "liberty-maven-plugin:~:start"
        )
        private val SUPPORTED_MAVEN_TEST_GOALS = listOf(
            "liberty:test-start",
            "liberty-maven-plugin:~:test-start"
        )

        private val ALL_GRADLE_TASKS = run {
            val list = SUPPORTED_GRADLE_TASKS.toMutableList()
            list.addAll(SUPPORTED_GRADLE_TEST_TASKS)
            list.toList()
        }
        private val ALL_MAVEN_GOALS = kotlin.run {
            val list = SUPPORTED_MAVEN_GOALS.toMutableList()
            list.addAll(SUPPORTED_MAVEN_TEST_GOALS)
            list.toList()
        }
    }


    override fun getOrder(): Int {
        return 1
    }

    override fun getPreferredUserFlavor(): Flavor {
        return Flavor.OpenLiberty
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
        val buildSystem = instrumentationService.getBuildSystem(configuration)
        return when (buildSystem) {
            BuildSystem.GRADLE -> hasSupportedGradleTasks(instrumentationService.getTaskNames(configuration), ALL_GRADLE_TASKS)
            BuildSystem.MAVEN -> hasSupportedMavenGoals(instrumentationService.getTaskNames(configuration), ALL_MAVEN_GOALS)
            BuildSystem.INTELLIJ -> false
        }
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
                .withOtelSdkDisabledEqualsFalse()
                .withOtelExporterEndpoint()
                .withMockitoSupport(isTest)
                .withServiceName(moduleResolver, parametersExtractor, serviceNameProvider)
                .withExtendedObservability()
                .withOtelDebug()
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


    override fun isTest(
        instrumentationService: RunConfigurationInstrumentationService,
        configuration: RunConfiguration,
        params: SimpleProgramParameters
    ): Boolean {
        val buildSystem = instrumentationService.getBuildSystem(configuration)
        return when (buildSystem) {
            BuildSystem.GRADLE -> hasSupportedGradleTasks(instrumentationService.getTaskNames(configuration), SUPPORTED_GRADLE_TEST_TASKS)
            BuildSystem.MAVEN -> hasSupportedMavenGoals(instrumentationService.getTaskNames(configuration), SUPPORTED_MAVEN_TEST_GOALS)
            BuildSystem.INTELLIJ -> false
        }
    }

    //actually not relevant because this flavor never calls withOtelAgent
    override fun useOtelAgent(): Boolean {
        return false
    }
}
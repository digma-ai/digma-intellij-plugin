package org.digma.intellij.plugin.idea.execution.flavor

import com.intellij.execution.JavaTestConfigurationBase
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
import org.digma.intellij.plugin.idea.execution.services.JavaServerRunConfigurationInstrumentationService


open class DefaultInstrumentationFlavor : BaseInstrumentationFlavor() {


    companion object {
        private val SUPPORTED_GRADLE_TASKS = listOf("main", "bootRun", "run")
        private val SUPPORTED_GRADLE_TEST_TASKS = listOf("test", "bootTestRun")
        private val SUPPORTED_MAVEN_GOALS = listOf(
            "exec:exec",
            "exec:java",
            "spring-boot:run",
            "spring-boot-maven-plugin:~:run",
            "spring-boot:start",
            "spring-boot-maven-plugin:~:start",
            "tomcat7:run",
            "tomcat7-maven-plugin:~:run",
            "tomcat7:run-war",
            "tomcat7-maven-plugin:~:run-war",
            "tomcat6:run",
            "tomcat6-maven-plugin:~:run",
            "tomcat6:run-war",
            "tomcat6-maven-plugin:~:run-war",
            "jetty:run",
            "jetty-maven-plugin:~:run",
            "jetty:run-war",
            "jetty-maven-plugin:~:run-war",
            "jetty:start",
            "jetty-maven-plugin:~:start",
            "jetty:start-war",
            "jetty-maven-plugin:~:start-war"
        )
        private val SUPPORTED_MAVEN_TEST_GOALS = listOf(
            "surefire:test",
            "maven-surefire-plugin:~:test",
            "spring-boot:test-run",
            "spring-boot-maven-plugin:~:test-run"
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

    //default should be the lowest order. so that we check more specific flavors first
    override fun getOrder(): Int {
        return Int.MAX_VALUE - 1
    }

    override fun getFlavor(): InstrumentationFlavorType {
        return InstrumentationFlavorType.Default
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
            BuildSystem.INTELLIJ -> validateServiceType(instrumentationService)
        }
    }

    //just make sure we're not making a mistake here. although JavaServerInstrumentationFlavor is higher order
    // then DefaultInstrumentationFlavor
    private fun validateServiceType(instrumentationService: RunConfigurationInstrumentationService): Boolean {
        return instrumentationService !is JavaServerRunConfigurationInstrumentationService
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
                .withOtelAgent(useOtelAgent())
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
    ): Map<String, String> {

        return try {
            val isTest = isTest(instrumentationService, configuration, params)

            otelResourceAttributesBuilder
                .withCommonResourceAttributes(isTest, parametersExtractor)
                .build()

        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("${this::class.java}.buildJavaToolOptions", e)
            mapOf()
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
            BuildSystem.INTELLIJ -> configuration is JavaTestConfigurationBase
        }
    }
}
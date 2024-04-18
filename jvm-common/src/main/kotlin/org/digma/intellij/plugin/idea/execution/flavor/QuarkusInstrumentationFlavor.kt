package org.digma.intellij.plugin.idea.execution.flavor

import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.openapi.module.Module
import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService
import org.digma.intellij.plugin.idea.execution.JavaToolOptionsBuilder
import org.digma.intellij.plugin.idea.execution.ModuleResolver
import org.digma.intellij.plugin.idea.execution.OtelResourceAttributesBuilder
import org.digma.intellij.plugin.idea.execution.ParametersExtractor
import org.digma.intellij.plugin.idea.execution.ProjectHeuristics
import org.digma.intellij.plugin.idea.execution.ServiceNameProvider
import org.digma.intellij.plugin.idea.execution.getModuleMetadata
import org.digma.intellij.plugin.idea.execution.services.JavaServerRunConfigurationInstrumentationService

class QuarkusInstrumentationFlavor : BaseInstrumentationFlavor() {

    companion object {
        private val SUPPORTED_GRADLE_TASKS = listOf("main", "quarkusDev", "quarkusRun")
        private val SUPPORTED_GRADLE_TEST_TASKS = listOf("quarkusIntTest", "quarkusTest")
        private val SUPPORTED_MAVEN_GOALS = listOf(
            "quarkus:dev",
            "quarkus-maven-plugin:~:dev",
            "quarkus:run",
            "quarkus-maven-plugin:~:run"
        )
        private val SUPPORTED_MAVEN_TEST_GOALS = listOf(
            "quarkus:test",
            "quarkus-maven-plugin:~:test"
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

    override fun getFlavor(): InstrumentationFlavorType {
        return InstrumentationFlavorType.Quarkus
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
        val applicable = when (buildSystem) {
            BuildSystem.GRADLE -> hasSupportedGradleTasks(instrumentationService.getTaskNames(configuration), ALL_GRADLE_TASKS)
            BuildSystem.MAVEN -> hasSupportedMavenGoals(instrumentationService.getTaskNames(configuration), ALL_MAVEN_GOALS)
            BuildSystem.INTELLIJ -> validateServiceType(instrumentationService)
        }

        if (!applicable) {
            return false
        }

        val module = moduleResolver.resolveModule()
        return isQuarkusTracing(module, projectHeuristics)
    }

    //just make sure we're not making a mistake here. it may be any service type but not JavaServerRunConfigurationInstrumentationService
    private fun validateServiceType(instrumentationService: RunConfigurationInstrumentationService): Boolean {
        return instrumentationService !is JavaServerRunConfigurationInstrumentationService
    }

    private fun isQuarkusTracing(module: Module?, projectHeuristics: ProjectHeuristics): Boolean {
        return isQuarkusModule(module) || projectHeuristics.hasOnlyQuarkusModules()
    }

    private fun isQuarkusModule(module: Module?): Boolean {
        val moduleMetadata = getModuleMetadata(module)
        return moduleMetadata?.hasQuarkus() ?: false
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
                .withQuarkusOtelExportedTracesEndpoint()
                .withQuarkusTest(isTest)
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
    ): Map<String, String> {

        return try {
            otelResourceAttributesBuilder
                .withUserId()
                .withScmCommitId()
                .build()

        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("${this::class.java}.buildJavaToolOptions", e)
            mapOf()
        }
    }


    //actually not relevant because this flavor never calls withOtelAgent
    override fun useOtelAgent(): Boolean {
        return false
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
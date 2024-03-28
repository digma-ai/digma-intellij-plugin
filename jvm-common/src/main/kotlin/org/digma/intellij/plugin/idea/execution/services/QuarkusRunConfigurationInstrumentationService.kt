package org.digma.intellij.plugin.idea.execution.services

import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.openapi.module.Module
import org.digma.intellij.plugin.analytics.LOCAL_TESTS_ENV
import org.digma.intellij.plugin.analytics.isCentralized
import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService
import org.digma.intellij.plugin.execution.RunConfigurationType
import org.digma.intellij.plugin.idea.deps.ModulesDepsService
import org.digma.intellij.plugin.idea.execution.ConfigurationCleaner
import org.digma.intellij.plugin.idea.execution.DIGMA_ENVIRONMENT_RESOURCE_ATTRIBUTE
import org.digma.intellij.plugin.idea.execution.ExternalSystemConfigurationCleaner
import org.digma.intellij.plugin.idea.execution.ExternalSystemJavaToolOptionsMerger
import org.digma.intellij.plugin.idea.execution.JavaToolOptionsBuilder
import org.digma.intellij.plugin.idea.execution.JavaToolOptionsMerger
import org.digma.intellij.plugin.idea.execution.ParametersExtractor
import org.digma.intellij.plugin.idea.execution.ServiceNameProvider
import org.digma.intellij.plugin.idea.externalsystem.findGradleRunConfigurationInstrumentationService
import org.digma.intellij.plugin.idea.externalsystem.findMavenRunConfigurationInstrumentationService

//don't change to light service because it will register always. we want it to register only for Idea
@Suppress("LightServiceMigrationCode")
class QuarkusRunConfigurationInstrumentationService : BaseJvmRunConfigurationInstrumentationService() {

    private val gradleInstrumentationService: RunConfigurationInstrumentationService? = findGradleRunConfigurationInstrumentationService()
    private val mavenInstrumentationService: RunConfigurationInstrumentationService? = findMavenRunConfigurationInstrumentationService()

    private fun isGradleConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        return gradleInstrumentationService?.isHandlingType(configuration) ?: false
    }

    private fun getGradleTasks(configuration: RunConfigurationBase<*>): Set<String> {
        return gradleInstrumentationService?.getTaskNames(configuration) ?: setOf()
    }

    private fun isMavenConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        return mavenInstrumentationService?.isHandlingType(configuration) ?: false
    }

    private fun getMavenGoals(configuration: RunConfigurationBase<*>): Set<String> {
        return mavenInstrumentationService?.getTaskNames(configuration) ?: setOf()
    }


    private fun isGradle(configuration: RunConfigurationBase<*>): Boolean {
        //TODO: support Quarkus with gradle
        return false
    }

    private fun isGradleTest(configuration: RunConfigurationBase<*>): Boolean {
        //TODO: support Quarkus with gradle
        return false
    }

    private fun isMaven(configuration: RunConfigurationBase<*>): Boolean {
        return isMavenConfiguration(configuration) && getMavenGoals(configuration).any {
            it == "quarkus:dev" ||
                    it == "quarkus:run" ||
                    (it.contains(":quarkus-maven-plugin:") && it.endsWith(":dev")) ||
                    (it.contains(":quarkus-maven-plugin:") && it.endsWith(":run"))
        }
    }

    private fun isMavenTest(configuration: RunConfigurationBase<*>): Boolean {
        return isMavenConfiguration(configuration) && getMavenGoals(configuration).any {
            it == "quarkus:test" ||
                    (it.contains(":quarkus-maven-plugin:") && it.endsWith(":test"))
        }
    }

    //we don't always have the params to resolve the module,
    // so this isJavaTest will return true for any java test not just quarkus.
    //its probably ok as long as isJavaTest(RunConfiguration,SimpleProgramParameters)
    //will return an accurate result because it has the params for module resolver
    private fun isJavaTest(configuration: RunConfiguration): Boolean {
        return configuration is JavaTestConfigurationBase
    }

    private fun isJavaTest(configuration: RunConfiguration, params: SimpleProgramParameters?): Boolean {
        return configuration is JavaTestConfigurationBase && params != null && isQuarkusModule(configuration, params)
    }

    private fun isQuarkusModule(configuration: JavaTestConfigurationBase, params: SimpleProgramParameters): Boolean {
        val module = getModuleResolver(configuration, params).resolveModule() ?: return false
        return ModulesDepsService.getInstance(module.project).isQuarkusModule(module)
    }

    private fun isHandlingConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        return isGradle(configuration) || isGradleTest(configuration) || isMaven(configuration) || isMavenTest(configuration)
    }

    //this may return true for any java test. it should not be used for instrumentation , only to find a handler
    // for reporting configuration details or other needs.
    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean {
        return isHandlingConfiguration(configuration) || isJavaTest(configuration)
    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): Boolean {
        return isHandlingConfiguration(configuration) || isJavaTest(configuration, params)
    }


    override fun getConfigurationType(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): RunConfigurationType {
        if (isHandlingConfiguration(configuration) || isJavaTest(configuration, params)) {
            return RunConfigurationType.Quarkus
        }
        return RunConfigurationType.Unknown
    }

    override fun isHandlingType(configuration: RunConfigurationBase<*>): Boolean {
        return isHandlingConfiguration(configuration) || isJavaTest(configuration)
    }

    override fun isTest(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters?): Boolean {
        return isGradleTest(configuration) || isMavenTest(configuration) || isJavaTest(configuration, params)
    }

    override fun shouldUseOtelAgent(resolvedModule: Module?): Boolean {
        return false
    }

    override fun isSpringBootMicrometerTracing(module: Module?): Boolean {
        return false
    }

    override fun isMicronautModule(module: Module?): Boolean {
        return false
    }


    override fun getJavaToolOptionsMerger(
        configuration: RunConfigurationBase<*>,
        params: SimpleProgramParameters,
        parametersExtractor: ParametersExtractor
    ): JavaToolOptionsMerger {
        if (isGradleConfiguration(configuration)) {
            return ExternalSystemJavaToolOptionsMerger(configuration, params, parametersExtractor)
        }
        return super.getJavaToolOptionsMerger(configuration, params, parametersExtractor)
    }

    override fun shouldCleanConfigurationAfterStart(configuration: RunConfigurationBase<*>): Boolean {
        return isGradleConfiguration(configuration)
    }

    override fun getConfigurationCleaner(configuration: RunConfigurationBase<*>): ConfigurationCleaner {
        if (isGradleConfiguration(configuration)) {
            return ExternalSystemConfigurationCleaner(configuration)
        }
        return super.getConfigurationCleaner(configuration)
    }

    override fun getTaskNames(configuration: RunConfigurationBase<*>): Set<String> {
        if (isGradleConfiguration(configuration)) {
            return getGradleTasks(configuration)
        } else if (isMavenConfiguration(configuration)) {
            return getMavenGoals(configuration)
        }
        return setOf()
    }

    override fun getBuildSystem(configuration: RunConfigurationBase<*>): BuildSystem {
        if (isGradleConfiguration(configuration)) {
            return BuildSystem.GRADLE
        } else if (isMavenConfiguration(configuration)) {
            return BuildSystem.MAVEN
        }
        return BuildSystem.INTELLIJ
    }

    override fun getJavaToolOptionsBuilder(
        configuration: RunConfigurationBase<*>,
        params: SimpleProgramParameters,
        parametersExtractor: ParametersExtractor,
        serviceNameProvider: ServiceNameProvider,
        runnerSettings: RunnerSettings?
    ): JavaToolOptionsBuilder {
        return QuarkusJavaToolOptionsBuilder(configuration, params, parametersExtractor, serviceNameProvider)
    }


    private class QuarkusJavaToolOptionsBuilder(
        configuration: RunConfigurationBase<*>,
        params: SimpleProgramParameters,
        parametersExtractor: ParametersExtractor,
        serviceNameProvider: ServiceNameProvider
    ) : JavaToolOptionsBuilder(configuration, params, parametersExtractor, serviceNameProvider) {

        //no need for common properties here
        override fun withCommonProperties(): JavaToolOptionsBuilder {
            return this
        }

        override fun getCommonOtelSystemProperties(): String {
            return ""
        }

        override fun withTest(isTest: Boolean): JavaToolOptionsBuilder {
            if (isTest && !isCentralized(configuration.project)) {
                val envPart = "$DIGMA_ENVIRONMENT_RESOURCE_ATTRIBUTE=$LOCAL_TESTS_ENV"
                javaToolOptions
                    .append("-Dquarkus.otel.resource.attributes=\"$envPart\"")
                    .append(" ")
                    .append("-Dquarkus.otel.bsp.schedule.delay=0.001S") // set delay to 1 millisecond
                    .append(" ")
                    .append("-Dquarkus.otel.bsp.max.export.batch.size=1") // by setting size of 1 it kind of disable
                    .append(" ")
            }
            return this
        }

        override fun build(): String {

            javaToolOptions
                .insert(0, " ")
                .insert(0, "-Dquarkus.otel.exporter.otlp.traces.endpoint=${getExporterUrl()}")
                .insert(0, " ")

            return super.build()
        }
    }


}
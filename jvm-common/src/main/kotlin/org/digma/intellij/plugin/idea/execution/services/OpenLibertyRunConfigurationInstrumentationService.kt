package org.digma.intellij.plugin.idea.execution.services

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.openapi.module.Module
import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService
import org.digma.intellij.plugin.execution.RunConfigurationType
import org.digma.intellij.plugin.idea.execution.ConfigurationCleaner
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
class OpenLibertyRunConfigurationInstrumentationService : BaseJvmRunConfigurationInstrumentationService() {

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
        return isGradleConfiguration(configuration) && getGradleTasks(configuration).any {
            it.contains("libertyDev") || it.contains("libertyRun") || it.contains("libertyStart")
        }
    }

    private fun isGradleTest(configuration: RunConfigurationBase<*>): Boolean {
        // currently no special task for liberty test
        return false
    }

    private fun isMaven(configuration: RunConfigurationBase<*>): Boolean {
        return isMavenConfiguration(configuration) && getMavenGoals(configuration).any {
            it == "liberty:dev" ||
                    it == "liberty:run" ||
                    it == "liberty:start" ||
                    (it.contains(":liberty-maven-plugin:") && it.endsWith(":dev")) ||
                    (it.contains(":liberty-maven-plugin:") && it.endsWith(":run")) ||
                    (it.contains(":liberty-maven-plugin:") && it.endsWith(":start"))
        }
    }

    private fun isMavenTest(configuration: RunConfigurationBase<*>): Boolean {
        return isMavenConfiguration(configuration) && getMavenGoals(configuration).any {
            it == "liberty:test-start" ||
                    (it.contains(":liberty-maven-plugin:") && it.endsWith(":test-start"))
        }
    }

    private fun isHandlingConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        return isGradle(configuration) || isGradleTest(configuration) || isMaven(configuration) || isMavenTest(configuration)
    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean {
        return isHandlingConfiguration(configuration)
    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): Boolean {
        return isHandlingConfiguration(configuration)
    }


    override fun getConfigurationType(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): RunConfigurationType {
        if (isHandlingConfiguration(configuration)) {
            return RunConfigurationType.OpenLiberty
        }
        return RunConfigurationType.Unknown
    }

    override fun isHandlingType(configuration: RunConfigurationBase<*>): Boolean {
        return isHandlingConfiguration(configuration)
    }

    override fun isTest(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters?): Boolean {
        return isGradleTest(configuration) || isMavenTest(configuration)
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
        return OpenLibertyJavaToolOptionsBuilder(configuration, params, parametersExtractor, serviceNameProvider)
    }


    private class OpenLibertyJavaToolOptionsBuilder(
        configuration: RunConfigurationBase<*>,
        params: SimpleProgramParameters,
        parametersExtractor: ParametersExtractor,
        serviceNameProvider: ServiceNameProvider
    ) : JavaToolOptionsBuilder(configuration, params, parametersExtractor, serviceNameProvider) {


        init {
            //add at the beginning.
            //can also override build and use insert
            withOtelSdkDisabled()
            withOtelExportedEndpoint()
        }

        //no need for common properties here
        override fun withCommonProperties(): JavaToolOptionsBuilder {
            return this
        }

        override fun getCommonOtelSystemProperties(): String {
            return ""
        }
    }

}
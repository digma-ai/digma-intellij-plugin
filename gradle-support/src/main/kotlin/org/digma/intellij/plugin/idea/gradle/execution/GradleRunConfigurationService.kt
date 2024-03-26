package org.digma.intellij.plugin.idea.gradle.execution

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.execution.RunConfigurationType
import org.digma.intellij.plugin.idea.execution.ConfigurationCleaner
import org.digma.intellij.plugin.idea.execution.ExternalSystemConfigurationCleaner
import org.digma.intellij.plugin.idea.execution.ExternalSystemJavaToolOptionsMerger
import org.digma.intellij.plugin.idea.execution.JavaToolOptionsMerger
import org.digma.intellij.plugin.idea.execution.ModuleResolver
import org.digma.intellij.plugin.idea.execution.ParametersExtractor
import org.digma.intellij.plugin.idea.execution.services.BaseJvmRunConfigurationInstrumentationService
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration


//don't change to light service because it will register always. we want it to register only if gradle is enabled.
// see org.digma.intellij-with-gradle.xml
@Suppress("LightServiceMigrationCode")
class GradleRunConfigurationService : BaseJvmRunConfigurationInstrumentationService() {

    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean {
        return isApplicable(configuration, null)
    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): Boolean {
        return isApplicable(configuration, params)
    }

    private fun isApplicable(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters?): Boolean {
        if (configuration is GradleRunConfiguration) {

            val taskNames = configuration.settings.taskNames

            val isMainMethod = taskNames.any {
                it.contains(".main")
            }
            val hasBootRun = taskNames.any {
                it.contains(":bootRun") || it.equals("bootRun")
            }

            //support for the run task of the java application plugin. https://docs.gradle.org/current/userguide/application_plugin.html
            val hasRun = taskNames.any {
                it.contains(":run") || it.equals("run")
            }

            if (isMainMethod || hasBootRun || hasRun || isTest(configuration, params)) {
                return true
            }
        }

        return false
    }


    override fun isTest(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters?): Boolean {
        return if (configuration is GradleRunConfiguration) {
            val taskNames = configuration.settings.taskNames
            taskNames.any {
                it.equals(":test") ||
                        it.endsWith(":test") ||
                        it.equals("test") ||
                        it.equals(":bootTestRun") ||
                        it.endsWith(":bootTestRun") ||
                        it.equals("bootTestRun")
            }
        } else {
            false
        }
    }


    override fun getConfigurationType(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): RunConfigurationType {
        return if (configuration is GradleRunConfiguration) {
            if (isTest(configuration, params)) {
                RunConfigurationType.GradleTest
            } else {
                RunConfigurationType.GradleRun
            }
        } else {
            RunConfigurationType.Unknown
        }
    }

    override fun isHandlingType(configuration: RunConfigurationBase<*>): Boolean {
        return configuration is GradleRunConfiguration
    }

    override fun getTaskNames(configuration: RunConfigurationBase<*>): Set<String> {
        if (configuration is GradleRunConfiguration) {
            return configuration.settings.taskNames.toSet()
        }
        return setOf()
    }

    override fun getBuildSystem(configuration: RunConfigurationBase<*>): BuildSystem {
        if (configuration is GradleRunConfiguration) {
            return BuildSystem.GRADLE
        }
        return BuildSystem.INTELLIJ
    }

    override fun shouldCleanConfigurationAfterStart(configuration: RunConfigurationBase<*>): Boolean {
        return true
    }


    override fun getModuleResolver(
        configuration: RunConfigurationBase<*>,
        params: SimpleProgramParameters,
        runnerSettings: RunnerSettings?
    ): ModuleResolver {
        return GradleModuleResolver(configuration, params)
    }


    override fun getJavaToolOptionsMerger(
        configuration: RunConfigurationBase<*>,
        params: SimpleProgramParameters,
        parametersExtractor: ParametersExtractor
    ): JavaToolOptionsMerger {
        return ExternalSystemJavaToolOptionsMerger(configuration, params, parametersExtractor)
    }

    override fun getConfigurationCleaner(configuration: RunConfigurationBase<*>): ConfigurationCleaner {
        return ExternalSystemConfigurationCleaner(configuration)
    }
}
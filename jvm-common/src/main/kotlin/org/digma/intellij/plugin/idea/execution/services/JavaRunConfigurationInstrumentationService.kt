package org.digma.intellij.plugin.idea.execution.services

import com.intellij.execution.JavaRunConfigurationBase
import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.execution.RunConfigurationType

//don't change to light service because it will register always. we want it to register only for Idea
@Suppress("LightServiceMigrationCode")
class JavaRunConfigurationInstrumentationService : BaseJvmRunConfigurationInstrumentationService() {

    private fun isHandlingConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        return configuration is ApplicationConfiguration || configuration is JavaTestConfigurationBase
    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean {
        return isHandlingConfiguration(configuration)
    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): Boolean {
        return isHandlingConfiguration(configuration)
    }

    override fun isTest(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters?): Boolean {
        return configuration is JavaTestConfigurationBase
    }

    override fun getConfigurationType(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): RunConfigurationType {
        if (configuration is JavaTestConfigurationBase) {
            return RunConfigurationType.JavaTest
        } else if (configuration is JavaRunConfigurationBase) {
            return RunConfigurationType.JavaRun
        }
        return RunConfigurationType.Unknown
    }

    override fun isHandlingType(configuration: RunConfigurationBase<*>): Boolean {
        return isHandlingConfiguration(configuration)
    }
}
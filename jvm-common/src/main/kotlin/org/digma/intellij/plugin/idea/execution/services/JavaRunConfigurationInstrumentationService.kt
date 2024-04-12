package org.digma.intellij.plugin.idea.execution.services

import com.intellij.execution.JavaRunConfigurationBase
import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.RunConfiguration
import org.digma.intellij.plugin.execution.RunConfigurationType

//don't change to light service because it will register always. we want it to register only for Idea
@Suppress("LightServiceMigrationCode")
class JavaRunConfigurationInstrumentationService : BaseJvmRunConfigurationInstrumentationService() {

    override fun isApplicableFor(configuration: RunConfiguration): Boolean {
        return configuration is ApplicationConfiguration || configuration is JavaTestConfigurationBase
    }

    override fun getConfigurationType(configuration: RunConfiguration): RunConfigurationType {
        return if (configuration is JavaTestConfigurationBase) {
            RunConfigurationType.JavaTest
        } else if (configuration is JavaRunConfigurationBase) {
            RunConfigurationType.Java
        } else {
            RunConfigurationType.Unknown
        }
    }

}
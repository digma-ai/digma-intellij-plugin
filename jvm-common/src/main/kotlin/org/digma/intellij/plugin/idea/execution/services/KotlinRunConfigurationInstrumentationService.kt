package org.digma.intellij.plugin.idea.execution.services

import com.intellij.execution.configurations.RunConfiguration
import org.digma.intellij.plugin.execution.RunConfigurationType
import org.digma.intellij.plugin.idea.psi.kotlin.isKotlinRunConfiguration

//TODO: separate kotlin module like gradle and maven
//don't change to light service because it will register always. we want it to register only for Idea
@Suppress("LightServiceMigrationCode")
class KotlinRunConfigurationInstrumentationService : BaseJvmRunConfigurationInstrumentationService() {

    override fun isApplicableFor(configuration: RunConfiguration): Boolean {
        return isKotlinRunConfiguration(configuration)
    }

    override fun getConfigurationType(configuration: RunConfiguration): RunConfigurationType {
        return if (isKotlinRunConfiguration(configuration)) {
            RunConfigurationType.Kotlin
        } else {
            RunConfigurationType.Unknown
        }
    }
}
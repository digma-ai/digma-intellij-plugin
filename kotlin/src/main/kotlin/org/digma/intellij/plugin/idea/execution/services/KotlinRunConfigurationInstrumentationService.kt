package org.digma.intellij.plugin.idea.execution.services

import com.intellij.execution.configurations.RunConfiguration
import org.digma.intellij.plugin.execution.RunConfigurationType
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration

//don't change to light service because it will register always. we want it to register only for Idea
@Suppress("LightServiceMigrationCode")
class KotlinRunConfigurationInstrumentationService : BaseJvmRunConfigurationInstrumentationService() {

    override fun isApplicableFor(configuration: RunConfiguration): Boolean {
        return configuration is KotlinRunConfiguration
    }

    override fun getConfigurationType(configuration: RunConfiguration): RunConfigurationType {
        return if (configuration is KotlinRunConfiguration) {
            RunConfigurationType.Kotlin
        } else {
            RunConfigurationType.Unknown
        }
    }
}
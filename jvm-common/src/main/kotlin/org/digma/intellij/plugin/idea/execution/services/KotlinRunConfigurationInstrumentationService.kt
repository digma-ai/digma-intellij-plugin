package org.digma.intellij.plugin.idea.execution.services

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.execution.RunConfigurationType
import org.digma.intellij.plugin.idea.psi.kotlin.isKotlinRunConfiguration

//TODO: separate kotlin module like gradle and maven
//don't change to light service because it will register always. we want it to register only for Idea
@Suppress("LightServiceMigrationCode")
class KotlinRunConfigurationInstrumentationService : BaseJvmRunConfigurationInstrumentationService() {

    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean {
        return isKotlinRunConfiguration(configuration)
    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): Boolean {
        return isKotlinRunConfiguration(configuration)
    }

    //todo: maybe necessary to implement kotlin test ?
    override fun isTest(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters?): Boolean {
        return false
    }

    override fun getConfigurationType(configuration: RunConfigurationBase<*>, params: SimpleProgramParameters): RunConfigurationType {
        if (isKotlinRunConfiguration(configuration)) {
            if (isTest(configuration, params)) {
                return RunConfigurationType.KotlinTest
            } else {
                return RunConfigurationType.KotlinRun
            }
        }
        return RunConfigurationType.Unknown
    }

    override fun isHandlingType(configuration: RunConfigurationBase<*>): Boolean {
        return isKotlinRunConfiguration(configuration)
    }
}
package org.digma.intellij.plugin.idea.runcfg

import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.openapi.module.Module

interface RunConfigurationWrapper {

    fun canWrap(configuration: RunConfiguration, module: Module?): Boolean {
        return getRunConfigType(configuration, module) != RunConfigType.Unknown
    }

    fun getRunConfigType(configuration: RunConfiguration, module: Module?): RunConfigType

    fun <T : RunConfigurationBase<*>?> updateJavaParameters(
        configuration: T & Any,
        params: JavaParameters,
        runnerSettings: RunnerSettings?,
        resolvedModule: Module?,
    )

    fun isGradleConfiguration(configuration: RunConfiguration): Boolean
}
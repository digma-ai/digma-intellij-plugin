package org.digma.intellij.plugin.idea.runcfg

import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.openapi.module.Module

interface IRunConfigurationWrapper {

    fun canWrap(configuration: RunConfigurationBase<*>, module: Module?): Boolean {
        return getRunConfigType(configuration, module) != RunConfigType.Unknown
    }

    fun getRunConfigType(configuration: RunConfigurationBase<*>, module: Module?): RunConfigType

    fun <T : RunConfigurationBase<*>?> updateJavaParameters(
        configuration: T & Any,
        params: JavaParameters,
        runnerSettings: RunnerSettings?,
        resolvedModule: Module?,
    )

    fun isGradleConfiguration(configuration: RunConfigurationBase<*>): Boolean
}
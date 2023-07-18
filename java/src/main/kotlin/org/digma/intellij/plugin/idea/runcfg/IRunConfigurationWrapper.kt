package org.digma.intellij.plugin.idea.runcfg

import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.openapi.module.Module

interface IRunConfigurationWrapper {
    fun canWrap(configuration: RunConfigurationBase<*>): Boolean

    fun getRunConfigType(configuration: RunConfigurationBase<*>): RunConfigType

    fun <T : RunConfigurationBase<*>?> updateJavaParameters(
        configuration: T & Any,
        params: JavaParameters,
        runnerSettings: RunnerSettings?,
        resolvedModule: Module?,
    )

    fun isGradleConfiguration(configuration: RunConfigurationBase<*>): Boolean
}
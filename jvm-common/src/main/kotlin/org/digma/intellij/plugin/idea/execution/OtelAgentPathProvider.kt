package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.execution.target.TargetEnvironmentsManager
import com.intellij.execution.wsl.target.WslTargetEnvironmentConfiguration
import com.intellij.openapi.components.service
import com.intellij.openapi.util.SystemInfo
import org.digma.intellij.plugin.common.FileUtils

class OtelAgentPathProvider(configuration: RunConfiguration) {

    val otelAgentPath: String?
    val digmaExtensionPath: String?
    val digmaAgentPath: String?

    init {

        //paths can be overrider with system properties for development purposes
        val tmpOtelAgentPath = System.getProperty("digma.otel.agent.override.path", OTELJarProvider.getInstance().getOtelAgentJarPath())
        val tmpDigmaExtensionPath =
            System.getProperty("digma.otel.extension.override.path", OTELJarProvider.getInstance().getDigmaAgentExtensionJarPath())
        val tmpDigmaAgentPath = System.getProperty("digma.agent.override.path", OTELJarProvider.getInstance().getDigmaAgentJarPath())

        if (tmpOtelAgentPath != null && tmpDigmaExtensionPath != null && tmpDigmaAgentPath != null) {
            if (isWsl(configuration)) {
                otelAgentPath = FileUtils.convertWinToWslPath(tmpOtelAgentPath)
                digmaExtensionPath = FileUtils.convertWinToWslPath(tmpDigmaExtensionPath)
                digmaAgentPath = FileUtils.convertWinToWslPath(tmpDigmaAgentPath)
            } else {
                otelAgentPath = tmpOtelAgentPath
                digmaExtensionPath = tmpDigmaExtensionPath
                digmaAgentPath = tmpDigmaAgentPath
            }
        } else {
            otelAgentPath = null
            digmaExtensionPath = null
            digmaAgentPath = null
        }

    }

    private fun isWsl(configuration: RunConfiguration): Boolean {
        if (!SystemInfo.isWindows)
            return false

        return isProjectUnderWsl(configuration) ||
                isConfigurationTargetWsl(configuration)
    }

    private fun isProjectUnderWsl(configuration: RunConfiguration): Boolean {
        return configuration.project.basePath?.startsWith("//wsl$/") == true ||
                configuration.project.basePath?.startsWith("//wsl.localhost/") == true
    }


    private fun isConfigurationTargetWsl(configuration: RunConfiguration): Boolean {

        if (configuration is RunConfigurationBase<*>) {

            val targets = TargetEnvironmentsManager.getInstance(configuration.project).targets.resolvedConfigs()
            val targetName = (configuration.state as? RunConfigurationOptions)?.remoteTarget ?: return false

            val target = targets.firstOrNull { it.displayName == targetName }
            if (target == null)
                return false

            if (target !is WslTargetEnvironmentConfiguration)
                return false

            return true
        }
        return false
    }


    fun hasAgentPath(): Boolean {
        return otelAgentPath != null && digmaExtensionPath != null
    }

    fun hasDigmaAgentPath(): Boolean {
        return digmaAgentPath != null
    }

}
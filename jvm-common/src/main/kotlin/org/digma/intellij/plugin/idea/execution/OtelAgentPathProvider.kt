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

    init {
        val tmpAgentPath = service<OTELJarProvider>().getOtelAgentJarPath()
        val tmpDigmaExtensionPath = service<OTELJarProvider>().getDigmaAgentExtensionJarPath()
        if (tmpAgentPath != null && tmpDigmaExtensionPath != null) {
            if (isWsl(configuration)) {
                otelAgentPath = FileUtils.convertWinToWslPath(tmpAgentPath)
                digmaExtensionPath = FileUtils.convertWinToWslPath(tmpDigmaExtensionPath)
            } else {
                otelAgentPath = tmpAgentPath
                digmaExtensionPath = tmpDigmaExtensionPath
            }
        } else {
            otelAgentPath = null
            digmaExtensionPath = null
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

}
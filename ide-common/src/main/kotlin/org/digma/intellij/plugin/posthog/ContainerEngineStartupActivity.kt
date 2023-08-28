package org.digma.intellij.plugin.posthog

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log

class ContainerEngineStartupActivity : StartupActivity {
    private val logger = Logger.getInstance(ContainerEngineStartupActivity::class.java)
    private val isWindows: Boolean = System.getProperty("os.name").startsWith("windows", true)

    enum class ContainerEngine {
        DOCKER,
        PODMAN,
        UNKNOWN
    }

    override fun runActivity(project: Project) {
        val containerPlatform = getInstalled(project).toString().lowercase()
        ActivityMonitor.getInstance(project).registerContainerEngine(containerPlatform)
    }

    private fun getInstalled(project: Project): ContainerEngine {
        if(getExecPath(project, "podman") != null)
            return ContainerEngine.PODMAN

        if (getExecPath(project, "docker") != null)
            return ContainerEngine.DOCKER

        return ContainerEngine.UNKNOWN
    }
    private fun getExecPath(project: Project, executable: String): String ? {
        val cmd = GeneralCommandLine(if (isWindows) "where" else "which")
        cmd.addParameter(executable);
        val cmdString = cmd.commandLineString
        try {
            val result =  ExecUtil.execAndReadLine(cmd)
            Log.log(logger::debug, "getExecPath: $cmdString result with: $result")
            return result
        } catch (ex: Exception) {
            Log.warnWithException(logger, ex, "Failed to run '{}'", cmdString)
            ErrorReporter.getInstance().reportError(project,"Failed to run '${cmdString}'", ex)
        }
        return null
    }
}
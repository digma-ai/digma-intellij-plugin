package org.digma.intellij.plugin.posthog

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.digma.intellij.plugin.log.Log
import java.util.concurrent.TimeUnit

class ContainerEngineStartupActivity : StartupActivity {
    private val timeoutInSec = 5L
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

        if (isInstalled(project, "podman"))
            return ContainerEngine.PODMAN

        if (isInstalled(project, "docker"))
            return ContainerEngine.DOCKER

        return ContainerEngine.UNKNOWN
    }

    private fun isInstalled(project: Project, command: String): Boolean {
        val pb = ProcessBuilder(if (isWindows) "where" else "which", command)
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD)
        pb.redirectError(ProcessBuilder.Redirect.DISCARD)

        var found = false
        try {
            val p = pb.start()
            if (p.waitFor(timeoutInSec, TimeUnit.SECONDS)) {
                found = p.exitValue() == 0
            }
            p.destroy()
        } catch (ex: Exception) {
            val commandLine = pb.command().joinToString(" ")
            Log.warnWithException(logger, project, ex, "Failed to run '{}'", commandLine)
            ActivityMonitor.getInstance(project).registerError(ex, "Failed to run '${commandLine}'")
        }

        return found
    }
}
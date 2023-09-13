package org.digma.intellij.plugin.docker

import com.fasterxml.jackson.databind.node.ArrayNode
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.common.JsonUtils
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.settings.SettingsState

private val logger = Logger.getInstance("org.digma.intellij.plugin.docker.DigmaInstallationDiscovery")



//this method should be called only if we know that local engine is not installed
internal fun discoverActualRunningEngine(project: Project): DigmaInstallationStatus {
    val hasConnection = BackendConnectionMonitor.getInstance(project).isConnectionOk()
    return discoverActualRunningEngine(hasConnection)
}

//this method should be called only if we know that local engine is not installed
internal fun discoverActualRunningEngine(hasConnection: Boolean): DigmaInstallationStatus {

    //currently, if there is no connection always return status with null type because we don't try to detect sleeping
    // containers. if there is no connection we probably will not find running containers.
    if (!hasConnection) {
        return DigmaInstallationStatus(false, null)
    }

    val isLocalEngineRunning = isLocalEngineRunning()
    val isAnyEngineRunning = isAnyEngineRunning()
    val isExtensionRunning = isExtensionRunning()

    if (isLocalEngineRunning) {
        return DigmaInstallationStatus(true, DigmaInstallationType.localEngine)
    } else if (isExtensionRunning) {
        return DigmaInstallationStatus(true, DigmaInstallationType.dockerDesktop)
    } else if (isAnyEngineRunning) {
        return DigmaInstallationStatus(true, DigmaInstallationType.dockerCompose)
    } else {
        if (SettingsState.getInstance().state?.apiUrl != SettingsState.DEFAULT_API_URL) {
            return DigmaInstallationStatus(true, DigmaInstallationType.remote)
        } else {
            return DigmaInstallationStatus(true, null)
        }
    }

}



private fun isLocalEngineRunning(): Boolean {

    try {

        if (!isInstalled(DOCKER_COMMAND)) {
            return false
        }

        val projectName = COMPOSE_FILE_DIR

        val dockerCmd = getDockerCommand()

        val cmd = GeneralCommandLine(dockerCmd, "container", "ls", "--format", "{{json .Names}}")
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)

        val processOutput: ProcessOutput = ExecUtil.execAndGetOutput(cmd, 5000)

        if (processOutput.exitCode == 0) {
            val output = processOutput.stdout
            val firstLine = output.split(System.lineSeparator()).firstOrNull()
            return firstLine?.replace("\"", "")?.startsWith(projectName, true) ?: false
        }

    } catch (ex: Exception) {
        Log.warnWithException(logger, ex, "Failed detect isLocalEngineRunning")
        ErrorReporter.getInstance().reportError("DigmaInstallationDiscovery.getAnyEngineState", ex)
    }
    return false

}

private fun isAnyEngineRunning(): Boolean {

    try {

        if (!isInstalled(DOCKER_COMMAND)) {
            return false
        }

        val dockerCmd = getDockerCommand()

        val cmd = GeneralCommandLine(dockerCmd, "container", "ls", "--format", "{{json .Names}}")
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)

        val processOutput: ProcessOutput = ExecUtil.execAndGetOutput(cmd, 5000)

        if (processOutput.exitCode == 0) {
            val output = processOutput.stdout
            return output.contains("digma", true)
        }

    } catch (ex: Exception) {
        Log.warnWithException(logger, ex, "Failed to detect isAnyEngineRunning")
        ErrorReporter.getInstance().reportError("DigmaInstallationDiscovery.getAnyEngineState", ex)
    }
    return false

}



private fun isExtensionRunning(): Boolean {

    try {

        if (!isInstalled(DOCKER_COMMAND)) {
            return false
        }

        val dockerCmd = getDockerCommand()

        val cmd = GeneralCommandLine(dockerCmd, "extension", "ls", "--format", "json")
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)

        val processOutput: ProcessOutput = ExecUtil.execAndGetOutput(cmd, 5000)

        if (processOutput.exitCode == 0) {
            val output = processOutput.stdout

            val extensions: ArrayNode = JsonUtils.readTree(output) as ArrayNode
            val digmaExtension = extensions.find { jsonNode -> jsonNode.get("image").asText().contains("/digma-docker-extension") }
            if (digmaExtension != null) {
                return digmaExtension.get("vm")?.get("status")?.asText()?.contains("Running", true) ?: false
            }
        }

    } catch (ex: Exception) {
        Log.warnWithException(logger, ex, "Failed to detect isExtensionRunning")
        ErrorReporter.getInstance().reportError("DigmaInstallationDiscovery.getExtensionState", ex)
    }
    return false

}

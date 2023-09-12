package org.digma.intellij.plugin.docker

import com.fasterxml.jackson.databind.JsonNode
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


private enum class EngineState { Installed, InstalledAndRunning, NotInstalled }


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
    } else if (isAnyEngineRunning) {
        return DigmaInstallationStatus(true, DigmaInstallationType.dockerCompose)
    } else if (isExtensionRunning) {
        return DigmaInstallationStatus(true, DigmaInstallationType.dockerDesktop)
    } else {
        if (SettingsState.getInstance().state?.apiUrl != SettingsState.DEFAULT_API_URL) {
            return DigmaInstallationStatus(true, DigmaInstallationType.remote)
        } else {
            return DigmaInstallationStatus(true, null)
        }
    }


//
//    if (!hasConnection) {
//        val anyEngineState = getAnyEngineState()
//        val extensionState = getExtensionState()
//        if (anyEngineState == EngineState.InstalledAndRunning || anyEngineState == EngineState.Installed) {
//            return DigmaInstallationStatus(false, DigmaInstallationType.dockerCompose)
//        } else if (extensionState == EngineState.InstalledAndRunning || extensionState == EngineState.Installed) {
//            return DigmaInstallationStatus(false, DigmaInstallationType.dockerDesktop)
//        } else {
//            if (SettingsState.getInstance().state?.apiUrl != SettingsState.DEFAULT_API_URL) {
//                return DigmaInstallationStatus(false, DigmaInstallationType.remote)
//            } else {
//                return DigmaInstallationStatus(false, null)
//            }
//        }
//    }
//
//    return DigmaInstallationStatus(false, null)


}

//internal fun discoverInstallationStatus(hasConnection: Boolean): DigmaInstallationStatus {
//
////    val localEngineState = getLocalEngineState()
//    val anyEngineState = getAnyEngineState()
//    val extensionState = getExtensionState()
//
//    return if (hasConnection) {
////        if (localEngineState == EngineState.InstalledAndRunning) {
////            DigmaInstallationStatus(true, DigmaInstallationType.LocalEngine)
////        } else
//        if (anyEngineState == EngineState.InstalledAndRunning) {
//            DigmaInstallationStatus(true, DigmaInstallationType.DockerCompose)
//        } else if (extensionState == EngineState.InstalledAndRunning) {
//            DigmaInstallationStatus(true, DigmaInstallationType.DockerDesktop)
//        } else {
//            DigmaInstallationStatus(true, DigmaInstallationType.Remote)
//        }
//    } else {
////        if (localEngineState == EngineState.InstalledAndRunning || localEngineState == EngineState.Installed) {
////            DigmaInstallationStatus(false, DigmaInstallationType.LocalEngine)
////        } else
//        if (anyEngineState == EngineState.InstalledAndRunning || anyEngineState == EngineState.Installed) {
//            DigmaInstallationStatus(false, DigmaInstallationType.DockerCompose)
//        } else if (extensionState == EngineState.InstalledAndRunning || extensionState == EngineState.Installed) {
//            DigmaInstallationStatus(false, DigmaInstallationType.DockerDesktop)
//        } else {
//            DigmaInstallationStatus(false, DigmaInstallationType.Unknown)
//        }
//    }
//}
//

//currently we discover if its local engine by checking the persistence flag
//fun getLocalEngineState(): EngineState {
//
//    if (!isInstalled(DOCKER_COMMAND)) {
//        return EngineState.NotInstalled
//    }
//
//    val projectName = COMPOSE_FILE_DIR
//
//    val dockerCmd = getDockerCommand()
//
//    val cmd = GeneralCommandLine(dockerCmd, "container", "ls", "--all", "--format", "json")
//        .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
//
//    try {
//        val processOutput: ProcessOutput = ExecUtil.execAndGetOutput(cmd, 5000)
//
//        if (processOutput.exitCode == 0) {
//            val output = processOutput.stdout
//
//            val containers: ArrayNode = JsonUtils.readTree(output) as ArrayNode
//            val digmaApiNode = containers.find { jsonNode -> jsonNode.get("Image").asText().contains("digma", true) }
//            if (digmaApiNode != null) {
//                val names: ArrayNode = digmaApiNode.get("Names") as ArrayNode
//                val myProjectName = names.find { jsonNode -> jsonNode.asText().contains(projectName) }
//                if (myProjectName != null) {
//                    val isRunning = digmaApiNode.get("State")?.asText()?.equals("running", true) ?: false
//                    return if (isRunning) EngineState.InstalledAndRunning else EngineState.Installed
//                }
//            }
//        }
//
//    } catch (ex: Exception) {
//        Log.warnWithException(logger, ex, "Failed to run '{}'", cmd.commandLineString)
//        ErrorReporter.getInstance().reportError("DigmaInstallationDiscovery.getLocalEngineState", ex)
//    }
//    return EngineState.NotInstalled
//
//}


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
            return output.contains(projectName, true)
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


private fun isNamesContains(node: JsonNode, valueToFind: String): Boolean {

    if (node is ArrayNode) {
        return node.any { jsonNode ->
            jsonNode.asText().contains(valueToFind, true)
        }
    }

    return node.asText().contains(valueToFind, true)

}



//private fun getAnyEngineState(): EngineState {
//
//    if (!isInstalled(DOCKER_COMMAND)) {
//        return EngineState.NotInstalled
//    }
//
//    val dockerCmd = getDockerCommand()
//
//    val cmd = GeneralCommandLine(dockerCmd, "container", "ls", "--all", "--format", "json")
//        .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
//
//    try {
//        val processOutput: ProcessOutput = ExecUtil.execAndGetOutput(cmd, 5000)
//
//        if (processOutput.exitCode == 0) {
//            val output = processOutput.stdout
//
//            val containers: ArrayNode = JsonUtils.readTree(output) as ArrayNode
//            //find digma-plugin-api that is not our local engine
//            val digmaApiNode = containers.find { jsonNode -> jsonNode.get("Image").asText().contains("digma", true) }
//            if (digmaApiNode != null) {
//                val isRunning = digmaApiNode.get("State")?.asText()?.equals("running", true) ?: false
//                return if (isRunning) EngineState.InstalledAndRunning else EngineState.Installed
//            }
//        }
//
//    } catch (ex: Exception) {
//        Log.warnWithException(logger, ex, "Failed to run '{}'", cmd.commandLineString)
//        ErrorReporter.getInstance().reportError("DigmaInstallationDiscovery.getAnyEngineState", ex)
//    }
//    return EngineState.NotInstalled
//
//}


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
                return digmaExtension.get("status")?.asText()?.contains("Running", true) ?: false
            }
        }

    } catch (ex: Exception) {
        Log.warnWithException(logger, ex, "Failed to detect isExtensionRunning")
        ErrorReporter.getInstance().reportError("DigmaInstallationDiscovery.getExtensionState", ex)
    }
    return false

}

//private fun getExtensionState(): EngineState {
//
//    if (!isInstalled(DOCKER_COMMAND)) {
//        return EngineState.NotInstalled
//    }
//
//    val dockerCmd = getDockerCommand()
//
//    val cmd = GeneralCommandLine(dockerCmd, "extension", "ls", "--format", "json")
//        .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
//
//    try {
//        val processOutput: ProcessOutput = ExecUtil.execAndGetOutput(cmd, 5000)
//
//        if (processOutput.exitCode == 0) {
//            val output = processOutput.stdout
//
//            val extensions: ArrayNode = JsonUtils.readTree(output) as ArrayNode
//            val digmaExtension = extensions.find { jsonNode -> jsonNode.get("image").asText().contains("/digma-docker-extension") }
//            if (digmaExtension != null) {
//                val isRunning = digmaExtension.get("status")?.asText()?.contains("Running", true) ?: false
//                return if (isRunning) EngineState.InstalledAndRunning else EngineState.Installed
//            }
//        }
//
//    } catch (ex: Exception) {
//        Log.warnWithException(logger, ex, "Failed to run '{}'", cmd.commandLineString)
//        ErrorReporter.getInstance().reportError("DigmaInstallationDiscovery.getExtensionState", ex)
//    }
//    return EngineState.NotInstalled
//
//}

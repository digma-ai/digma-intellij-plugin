package org.digma.intellij.plugin.docker

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import org.digma.intellij.plugin.log.Log

val WHICH_COMMAND = if (SystemInfo.isWindows) "where" else "which"
const val DOCKER_COMMAND = "docker"
const val DOCKER_COMPOSE_COMMAND = "docker-compose"

private val logger = Logger.getInstance("org.digma.intellij.plugin.docker.CmdHelper")

fun getCommand(program: String): String? {
    val cmd = GeneralCommandLine(WHICH_COMMAND, program)
        .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)

    try {
        val result = ExecUtil.execAndReadLine(cmd)
        Log.log(logger::info, "getExecPath: {} result with: {}", cmd.commandLineString, result)
        return result
    } catch (ex: Exception) {
        Log.warnWithException(logger, ex, "Failed to run '{}'", cmd.commandLineString)
    }
    return null
}


fun getDockerCommand(): String? {
    var dockerCmd = getCommand(DOCKER_COMMAND)
    if (dockerCmd != null) {
        if (SystemInfo.isWindows && !dockerCmd.endsWith("exe", true)) {
            dockerCmd = dockerCmd.plus(".exe")
        }
        return dockerCmd
    }
    return null
}


fun getDockerComposeCommand(): List<String>? {
    var dockerComposeCmd = getCommand(DOCKER_COMPOSE_COMMAND)
    if (dockerComposeCmd != null) {
        if (SystemInfo.isWindows && !dockerComposeCmd.endsWith("exe", true)) {
            dockerComposeCmd = dockerComposeCmd.plus(".exe")
        }
        return listOf(dockerComposeCmd)
    }

    var dockerCmd = getCommand(DOCKER_COMMAND)
    if (dockerCmd != null) {
        if (SystemInfo.isWindows && !dockerCmd.endsWith("exe", true)) {
            dockerCmd = dockerCmd.plus(".exe")
        }
        return listOf(dockerCmd, "compose")
    }
    return null
}


fun isInstalled(program: String): Boolean {
    val cmd = GeneralCommandLine(WHICH_COMMAND, program)
        .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)

    try {
        val result = ExecUtil.execAndReadLine(cmd)
        Log.log(logger::info, "getExecPath: {} result with: {}", cmd.commandLineString, result)
        return result != null
    } catch (ex: Exception) {
        Log.warnWithException(logger, ex, "Failed to run '{}'", cmd.commandLineString)
    }
    return false
}
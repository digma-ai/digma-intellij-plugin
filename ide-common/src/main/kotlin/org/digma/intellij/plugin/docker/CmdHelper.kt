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




fun getDockerCommand(): String? {
    Log.log(logger::debug, "getDockerCommand invoked")
    return getCommand(DOCKER_COMMAND)
}


fun getDockerComposeCommand(): List<String>? {

    Log.log(logger::debug, "getDockerComposeCommand invoked")

    val dockerComposeCmd = getCommand(DOCKER_COMPOSE_COMMAND)
    if (dockerComposeCmd != null) {
        return listOf(dockerComposeCmd)
    }

    val dockerCmd = getCommand(DOCKER_COMMAND)
    if (dockerCmd != null) {
        return listOf(dockerCmd, "compose")
    }
    return null
}


private fun getCommand(program: String): String? {

    Log.log(logger::debug, "getCommand invoked for {}", program)

    val cmd = GeneralCommandLine(WHICH_COMMAND, program)
        .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)

    try {
        val result = if (SystemInfo.isWindows) {
            //on windows the result of 'where docker' may return two lines, ../docker and ..docker.exe
            val output = ExecUtil.execAndGetOutput(cmd).stdoutLines
            //prefer line that ends with exe or bat, or just the first one if none found
            output.first { it.endsWith(".exe", true) || it.endsWith(".bat", true) } ?: output.first()
        }else{
            ExecUtil.execAndReadLine(cmd)
        }

        Log.log(logger::debug, "getCommand {} result with {}", cmd.commandLineString, result)
        return result
    } catch (ex: Exception) {
        Log.warnWithException(logger, ex, "getCommand Failed to run '{}'", cmd.commandLineString)
    }
    return null
}


fun isInstalled(program: String): Boolean {

    Log.log(logger::debug, "isInstalled invoked for {}", program)

    val cmd = GeneralCommandLine(WHICH_COMMAND, program)
        .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)

    try {
        val result = ExecUtil.execAndReadLine(cmd)
        Log.log(logger::debug, "isInstalled.getExecPath: {} result with: {}", cmd.commandLineString, result)
        return result != null
    } catch (ex: Exception) {
        Log.warnWithException(logger, ex, "isInstalled Failed to run '{}'", cmd.commandLineString)
    }
    return false
}
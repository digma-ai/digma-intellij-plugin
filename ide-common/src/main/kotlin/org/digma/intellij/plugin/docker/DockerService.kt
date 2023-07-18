package org.digma.intellij.plugin.docker

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import org.digma.intellij.plugin.log.Log


@Service(Service.Level.APP)
class DockerService {

    private val logger = Logger.getInstance(this::class.java)

    private val engine = Engine()
    private val downloader = Downloader()

    companion object {
        val WHICH_COMMAND = if (SystemInfo.isWindows) "where" else "which"
        const val DOCKER_COMMAND = "docker"
        const val DOCKER_COMPOSE_COMMAND = "docker-compose"
    }


    init {
        if (!downloader.findComposeFile()) {
            downloader.downloadComposeFile()
        }
    }


    fun isDockerInstalled(): Boolean {
        return isInstalled(DOCKER_COMMAND) && isInstalled(DOCKER_COMPOSE_COMMAND)
    }


    private fun isInstalled(program: String): Boolean {
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

    private fun getCommand(program: String): String? {
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


    fun installEngine(): Boolean {
        if (downloader.downloadComposeFile()) {
            val dockerComposeCmd = getCommand(DOCKER_COMPOSE_COMMAND)
            return dockerComposeCmd?.let {
                return engine.up(downloader.composeFile!!, it)
            } ?: false
        }
        return false
    }

    fun stopEngine(): Boolean {
        val dockerComposeCmd = getCommand(DOCKER_COMPOSE_COMMAND)
        return dockerComposeCmd?.let {
            return engine.down(downloader.composeFile!!, it)
        } ?: false
    }

    fun startEngine(): Boolean {
        val dockerComposeCmd = getCommand(DOCKER_COMPOSE_COMMAND)
        return dockerComposeCmd?.let {
            return engine.up(downloader.composeFile!!, it)
        } ?: false
    }

    fun removeEngine(): Boolean {
        val dockerComposeCmd = getCommand(DOCKER_COMPOSE_COMMAND)
        return dockerComposeCmd?.let {
            engine.down(downloader.composeFile!!, it)
            return engine.remove(downloader.composeFile!!, it)
        } ?: false
    }


}
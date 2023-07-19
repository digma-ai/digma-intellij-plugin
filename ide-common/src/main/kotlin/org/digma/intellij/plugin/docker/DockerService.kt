package org.digma.intellij.plugin.docker

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import org.digma.intellij.plugin.analytics.BackendConnectionUtil
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.log.Log
import java.util.function.Consumer


@Service(Service.Level.APP)
class DockerService {

    private val logger = Logger.getInstance(this::class.java)

    private val engine = Engine()
    private val downloader = Downloader()

    companion object {
        val WHICH_COMMAND = if (SystemInfo.isWindows) "where" else "which"
        const val DOCKER_COMMAND = "docker"
        const val DOCKER_COMPOSE_COMMAND = "docker-compose"
        const val NO_DOCKER_COMPOSE_COMMAND = "no docker-compose command"
    }


    fun isDockerInstalled(): Boolean {
        return isInstalled(DOCKER_COMMAND)
    }

    fun isDockerComposeInstalled(): Boolean {
        return isInstalled(DOCKER_COMPOSE_COMMAND)
    }

    fun isEngineInstalled(): Boolean {
        return downloader.findComposeFile()
    }

    fun isEngineRunning(project: Project): Boolean {
        return BackendConnectionUtil.getInstance(project).testConnectionToBackend()
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


    private fun getDockerComposeCommand(): List<String>? {
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


    fun installEngine(project: Project, resultTask: Consumer<String>) {

        Backgroundable.runInNewBackgroundThread(project, "installing digma engine") {

            if (downloader.downloadComposeFile()) {
                val dockerComposeCmd = getDockerComposeCommand()

                if (dockerComposeCmd != null) {
                    val exitValue = engine.up(downloader.composeFile!!, dockerComposeCmd)
                    if (exitValue != "0") {
                        Log.log(logger::warn, "error installing engine {}", exitValue)
                        downloader.deleteFile()
                    }
                    notifyResult(exitValue, resultTask)
                } else {
                    Log.log(logger::warn, "could not find docker compose command")
                    downloader.deleteFile()
                    notifyResult(NO_DOCKER_COMPOSE_COMMAND, resultTask)
                }
            } else {
                Log.log(logger::warn, "Failed to download compose file")
                notifyResult("Failed to download compose file", resultTask)
            }
        }
    }

    fun upgradeEngine(project: Project) {

        Backgroundable.runInNewBackgroundThread(project, "upgrading digma engine") {

            if (downloader.downloadComposeFile()) {
                val dockerComposeCmd = getDockerComposeCommand()

                if (dockerComposeCmd != null) {
                    val exitValue = engine.up(downloader.composeFile!!, dockerComposeCmd)
                    if (exitValue != "0") {
                        Log.log(logger::warn, "error upgrading engine {}", exitValue)
                        downloader.deleteFile()
                    }
                } else {
                    Log.log(logger::warn, "could not find docker compose command")
                    downloader.deleteFile()
                }
            } else {
                Log.log(logger::warn, "Failed to download compose file")
            }
        }
    }

    fun stopEngine(project: Project, resultTask: Consumer<String>) {

        Backgroundable.runInNewBackgroundThread(project, "stopping digma engine") {

            if (downloader.findComposeFile()) {
                val dockerComposeCmd = getDockerComposeCommand()

                if (dockerComposeCmd != null) {
                    val exitValue = engine.stop(downloader.composeFile!!, dockerComposeCmd)
                    if (exitValue != "0") {
                        Log.log(logger::warn, "error stopping engine {}", exitValue)
                    }
                    notifyResult(exitValue, resultTask)
                } else {
                    Log.log(logger::warn, "could not find docker compose command")
                    downloader.deleteFile()
                    notifyResult(NO_DOCKER_COMPOSE_COMMAND, resultTask)
                }
            } else {
                Log.log(logger::warn, "Failed to find compose file")
                notifyResult("Failed to find compose file", resultTask)
            }
        }
    }

    fun startEngine(project: Project, resultTask: Consumer<String>) {

        Backgroundable.runInNewBackgroundThread(project, "starting digma engine") {

            if (!downloader.findComposeFile()) {
                downloader.downloadComposeFile()
            }

            if (downloader.findComposeFile()) {
                val dockerComposeCmd = getDockerComposeCommand()

                if (dockerComposeCmd != null) {
                    //we try to detect errors when running the docker command. engine.start executes docker-compose up,
                    // if executing docker-compose up while containers exist it will print many errors that are ok but
                    // that interferes with our attempt to detect errors.
                    //so running down and then up solves it
                    engine.down(downloader.composeFile!!, dockerComposeCmd)
                    try {
                        Thread.sleep(2000)
                    } catch (e: Exception) {
                        //ignore
                    }

                    val exitValue = engine.start(downloader.composeFile!!, dockerComposeCmd)
                    if (exitValue != "0") {
                        Log.log(logger::warn, "error starting engine {}", exitValue)
                        downloader.deleteFile()
                    }
                    notifyResult(exitValue, resultTask)
                } else {
                    Log.log(logger::warn, "could not find docker compose command")
                    downloader.deleteFile()
                    notifyResult(NO_DOCKER_COMPOSE_COMMAND, resultTask)
                }
            } else {
                Log.log(logger::warn, "Failed to find compose file")
                notifyResult("Failed to find compose file", resultTask)
            }
        }

    }


    fun removeEngine(project: Project, resultTask: Consumer<String>) {

        Backgroundable.runInNewBackgroundThread(project, "uninstalling digma engine") {

            if (!downloader.findComposeFile()) {
                downloader.downloadComposeFile()
            }

            if (downloader.findComposeFile()) {
                val dockerComposeCmd = getDockerComposeCommand()

                if (dockerComposeCmd != null) {
                    val exitValue = engine.remove(downloader.composeFile!!, dockerComposeCmd)
                    if (exitValue != "0") {
                        Log.log(logger::warn, "error uninstalling engine {}", exitValue)
                    }
                    notifyResult(exitValue, resultTask)
                } else {
                    Log.log(logger::warn, "could not find docker compose command")
                    notifyResult(NO_DOCKER_COMPOSE_COMMAND, resultTask)
                }

                //always delete fine here, it's an uninstallation
                downloader.deleteFile()

            } else {
                Log.log(logger::warn, "Failed to find compose file")
                notifyResult("Failed to find compose file", resultTask)
            }
        }
    }


    private fun notifyResult(errorMsg: String, resultTask: Consumer<String>) {
        resultTask.accept(errorMsg)
    }

}
package org.digma.intellij.plugin.docker

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageConstants
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.util.function.Consumer
import java.util.function.Supplier


@Service(Service.Level.APP)
class DockerService {

    private val logger = Logger.getInstance(this::class.java)

    private val engine = Engine()
    private val downloader = Downloader()
    private var installationInProgress: Boolean = false
    companion object {
        val WHICH_COMMAND = if (SystemInfo.isWindows) "where" else "which"
        const val DOCKER_COMMAND = "docker"
        const val DOCKER_COMPOSE_COMMAND = "docker-compose"
        const val NO_DOCKER_COMPOSE_COMMAND = "no docker-compose command"

        @JvmStatic
        fun getInstance():DockerService{
            return service<DockerService>()
        }
    }


    init {

        //todo: fix to delete old compose file, remove after few releases
        if (downloader.findOldComposeFile() && PersistenceService.getInstance().isLocalEngineInstalled() == null){
            PersistenceService.getInstance().setLocalEngineInstalled(true)
        }else if (PersistenceService.getInstance().isLocalEngineInstalled() == null){
            PersistenceService.getInstance().setLocalEngineInstalled(false)
        }

        downloader.deleteOldFileIfExists()

        if (isEngineInstalled()){
            //this will happen on IDE start,
            // DockerService is an application service so Downloader will be created once per application
            downloader.unpackAndTryDownloadLatest()
        }
    }



    fun isInstallationInProgress(): Boolean{
        return installationInProgress
    }

    fun isDockerInstalled(): Boolean {
        return isInstalled(DOCKER_COMMAND) || isInstalled(DOCKER_COMPOSE_COMMAND)
    }


    fun isEngineInstalled(): Boolean {
        return PersistenceService.getInstance().isLocalEngineInstalled()!!
    }

    fun isEngineRunning(project: Project): Boolean {
        return isEngineInstalled() && BackendConnectionMonitor.getInstance(project).isConnectionOk()
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


    private fun isDockerDaemonDownExitValue(exitValue: String): Boolean {
        return exitValue.contains("Cannot connect to the Docker daemon", true) ||//mac, linux
                exitValue.contains("docker daemon is not running", true)//win
    }

    fun installEngine(project: Project, resultTask: Consumer<String>) {
        installationInProgress = true

        val onCompleted = Consumer { _: String ->
            installationInProgress = false
        }.andThen(resultTask)

        ActivityMonitor.getInstance(project).registerDigmaEngineEventStart("installEngine", mapOf())

        Backgroundable.runInNewBackgroundThread(project, "installing digma engine") {

            try {

                if (downloader.downloadComposeFile(true)) {
                    val dockerComposeCmd = getDockerComposeCommand()

                    if (dockerComposeCmd != null) {

                        var exitValue = engine.up(project, downloader.composeFile, dockerComposeCmd)
                        if (exitValue != "0") {
                            Log.log(logger::warn, "error installing engine {}", exitValue)
                            if (isDockerDaemonDownExitValue(exitValue)) {
                                exitValue = doRetryFlowWhenDockerDaemonIsDown(project) {
                                    engine.up(project, downloader.composeFile, dockerComposeCmd)
                                }
                            }
                        }

                        if (exitValue == "0") {
                            PersistenceService.getInstance().setLocalEngineInstalled(true)
                        }

                        notifyResult(exitValue, onCompleted)
                    } else {
                        ActivityMonitor.getInstance(project).registerDigmaEngineEventError("installEngine", "could not find docker compose command")
                        Log.log(logger::warn, "could not find docker compose command")
                        notifyResult(NO_DOCKER_COMPOSE_COMMAND, onCompleted)
                    }
                } else {
                    ActivityMonitor.getInstance(project).registerDigmaEngineEventError("installEngine", "Failed to download compose file")
                    Log.log(logger::warn, "Failed to download compose file")
                    notifyResult("Failed to download compose file", onCompleted)
                }

            }catch (e:Exception){
                ActivityMonitor.getInstance(project).registerDigmaEngineEventError("installEngine", "Failed in installEngine $e")
                Log.warnWithException(logger,e, "Failed install docker engine {}",e)
                notifyResult("Failed to install docker engine: $e", onCompleted)
            }finally {
                ActivityMonitor.getInstance(project).registerDigmaEngineEventEnd("installEngine", mapOf())
            }
        }
    }

    fun upgradeEngine(project: Project) {

        ActivityMonitor.getInstance(project).registerDigmaEngineEventStart("upgradeEngine", mapOf())

        Backgroundable.runInNewBackgroundThread(project, "upgrading digma engine") {

            try {
                if (downloader.downloadComposeFile(true)) {
                    val dockerComposeCmd = getDockerComposeCommand()

                    if (dockerComposeCmd != null) {
                        val exitValue = engine.up(project, downloader.composeFile, dockerComposeCmd)
                        if (exitValue != "0") {
                            Log.log(logger::warn, "error upgrading engine {}", exitValue)
                        }
                    } else {
                        ActivityMonitor.getInstance(project).registerDigmaEngineEventError("upgradeEngine", "could not find docker compose command")
                        Log.log(logger::warn, "could not find docker compose command")
                    }
                } else {
                    ActivityMonitor.getInstance(project).registerDigmaEngineEventError("upgradeEngine", "Failed to download compose file")
                    Log.log(logger::warn, "Failed to download compose file")
                }
            }catch (e: Exception){
                ActivityMonitor.getInstance(project).registerDigmaEngineEventError("upgradeEngine", "Failed in upgradeEngine $e")
                Log.warnWithException(logger,e, "Failed install docker engine {}",e)
            }finally {
                ActivityMonitor.getInstance(project).registerDigmaEngineEventEnd("upgradeEngine", mapOf())
            }
        }
    }


    fun stopEngine(project: Project, resultTask: Consumer<String>) {

        ActivityMonitor.getInstance(project).registerDigmaEngineEventStart("stopEngine", mapOf())

        Backgroundable.runInNewBackgroundThread(project, "stopping digma engine") {

            try {
                if (downloader.downloadComposeFile()) {

                    val dockerComposeCmd = getDockerComposeCommand()

                    if (dockerComposeCmd != null) {

                        val exitValue = engine.stop(project, downloader.composeFile, dockerComposeCmd)
                        if (exitValue != "0") {
                            Log.log(logger::warn, "error stopping engine {}", exitValue)
                        }
                        notifyResult(exitValue, resultTask)
                    } else {
                        ActivityMonitor.getInstance(project).registerDigmaEngineEventError("stopEngine", "could not find docker compose command")
                        Log.log(logger::warn, "could not find docker compose command")
                        notifyResult(NO_DOCKER_COMPOSE_COMMAND, resultTask)
                    }
                } else {
                    ActivityMonitor.getInstance(project).registerDigmaEngineEventError("stopEngine", "Failed to download compose file")
                    Log.log(logger::warn, "Failed to download compose file")
                    notifyResult("Failed to download compose file", resultTask)
                }
            }catch (e:Exception){
                ActivityMonitor.getInstance(project).registerDigmaEngineEventError("stopEngine", "Failed in stopEngine $e")
                Log.warnWithException(logger,e, "Failed to stop docker engine {}",e)
                notifyResult("Failed to stop docker engine: $e", resultTask)
            }finally {
                ActivityMonitor.getInstance(project).registerDigmaEngineEventEnd("stopEngine", mapOf())
            }
        }
    }

    fun startEngine(project: Project, resultTask: Consumer<String>) {

        ActivityMonitor.getInstance(project).registerDigmaEngineEventStart("startEngine", mapOf())

        Backgroundable.runInNewBackgroundThread(project, "starting digma engine") {

            try {
                if (downloader.downloadComposeFile()) {
                    val dockerComposeCmd = getDockerComposeCommand()

                    if (dockerComposeCmd != null) {
                        //we try to detect errors when running the docker command. engine.start executes docker-compose up,
                        // if executing docker-compose up while containers exist it will print many errors that are ok but
                        // that interferes with our attempt to detect errors.
                        //so running down and then up solves it
                        engine.down(project, downloader.composeFile, dockerComposeCmd, false)
                        try {
                            Thread.sleep(2000)
                        } catch (e: Exception) {
                            //ignore
                        }

                        var exitValue = engine.start(project, downloader.composeFile, dockerComposeCmd)
                        if (exitValue != "0") {
                            Log.log(logger::warn, "error starting engine {}", exitValue)
                            if (isDockerDaemonDownExitValue(exitValue)) {
                                exitValue = doRetryFlowWhenDockerDaemonIsDown(project) {
                                    engine.start(project, downloader.composeFile, dockerComposeCmd)
                                }
                            }
                        }

                        notifyResult(exitValue, resultTask)
                    } else {
                        ActivityMonitor.getInstance(project).registerDigmaEngineEventError("startEngine", "could not find docker compose command")
                        Log.log(logger::warn, "could not find docker compose command")
                        notifyResult(NO_DOCKER_COMPOSE_COMMAND, resultTask)
                    }
                } else {
                    ActivityMonitor.getInstance(project).registerDigmaEngineEventError("startEngine", "Failed to download compose file")
                    Log.log(logger::warn, "Failed to download compose file")
                    notifyResult("Failed to download compose file", resultTask)
                }
            }catch (e:Exception){
                ActivityMonitor.getInstance(project).registerDigmaEngineEventError("startEngine", "Failed in startEngine $e")
                Log.warnWithException(logger,e, "Failed to start docker engine {}",e)
                notifyResult("Failed to start docker engine: $e", resultTask)
            }finally {
                ActivityMonitor.getInstance(project).registerDigmaEngineEventEnd("startEngine", mapOf())
            }
        }

    }


    fun removeEngine(project: Project, resultTask: Consumer<String>) {

        ActivityMonitor.getInstance(project).registerDigmaEngineEventStart("removeEngine", mapOf())

        Backgroundable.runInNewBackgroundThread(project, "uninstalling digma engine") {

            try {
                if (downloader.downloadComposeFile()) {
                    val dockerComposeCmd = getDockerComposeCommand()

                    if (dockerComposeCmd != null) {
                        val exitValue = engine.remove(project, downloader.composeFile, dockerComposeCmd)
                        if (exitValue != "0") {
                            Log.log(logger::warn, "error uninstalling engine {}", exitValue)
                        }
                        notifyResult(exitValue, resultTask)
                    } else {
                        ActivityMonitor.getInstance(project).registerDigmaEngineEventError("removeEngine", "could not find docker compose command")
                        Log.log(logger::warn, "could not find docker compose command")
                        notifyResult(NO_DOCKER_COMPOSE_COMMAND, resultTask)
                    }

                    //always delete file here, it's an uninstallation
                    downloader.deleteFile()

                    PersistenceService.getInstance().setLocalEngineInstalled(false)

                } else {
                    ActivityMonitor.getInstance(project).registerDigmaEngineEventError("removeEngine", "Failed to download compose file")
                    Log.log(logger::warn, "Failed to download compose file")
                    notifyResult("Failed to download compose file", resultTask)
                }
            }catch (e:Exception){
                ActivityMonitor.getInstance(project).registerDigmaEngineEventError("removeEngine", "failed in removeEngine $e")
                Log.warnWithException(logger,e, "Failed to remove docker engine {}",e)
                notifyResult("Failed to remove docker engine: $e", resultTask)
            }finally {
                ActivityMonitor.getInstance(project).registerDigmaEngineEventEnd("removeEngine", mapOf())
            }
        }
    }


    private fun doRetryFlowWhenDockerDaemonIsDown(project: Project, runCommand: Supplier<String>): String {

        val eventName = "docker-daemon-is-down"

        ActivityMonitor.getInstance(project).registerCustomEvent(eventName, null)

        tryStartDockerDaemon(project)

        ActivityMonitor.getInstance(project).registerCustomEvent(eventName, mapOf("action" to "retry triggered by system"))
        var exitValue = runCommand.get()

        if (isDockerDaemonDownExitValue(exitValue)) {
            var res = MessageConstants.YES
            ApplicationManager.getApplication().invokeAndWait {
                res = Messages.showYesNoDialog(
                    project,
                    "Please make sure the Docker daemon is running\n" +
                            "Once the Docker daemon is running, press the retry button.\n",
                    "Digma engine failed to run",
                    "Retry",
                    "Cancel",
                    null
                )
            }
            if (res == MessageConstants.YES) {
                ActivityMonitor.getInstance(project).registerCustomEvent(eventName, mapOf("action" to "retry triggered by user"))
                exitValue = runCommand.get()
                if (isDockerDaemonDownExitValue(exitValue)) {
                    ActivityMonitor.getInstance(project).registerCustomEvent(eventName, null)
                    ApplicationManager.getApplication().invokeAndWait {
                        Messages.showMessageDialog(project, "Digma engine failed to run\nDocker daemon is down", "", null)
                    }
                }
            } else {
                ActivityMonitor.getInstance(project).registerCustomEvent(eventName, mapOf("action" to "retry canceled by user"))
            }
        }


        return exitValue
    }

    private fun tryStartDockerDaemon(project: Project) {

        Log.log(logger::info, "Trying to start docker daemon")

        val command = if (SystemInfo.isMac) {
            listOf("docker-machine", "restart")
        } else if (SystemInfo.isWindows) {
            listOf("wsl.exe", "-u", "root", "-e", "sh", "-c", "service docker status || service docker start")
        } else if (SystemInfo.isLinux) {
            listOf("systemctl", "start", "docker.service")
        } else {
            listOf("docker", "start")//just a useless fallback to satisfy kotlin
        }

        val cmd = GeneralCommandLine(command)
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)

        try {
            Log.log(logger::info, "executing command: {}", cmd.commandLineString)
            val processOutput = ExecUtil.execAndGetOutput(cmd, 10000)
            val output = "exitCode:${processOutput.exitCode}, stdout:${processOutput.stdout}, stderr:${processOutput.stderr}"
            ActivityMonitor.getInstance(project).registerCustomEvent("Engine.start-docker-daemon", mapOf("result" to output))
            Log.log(logger::info, "start docker command result: {}", output)
        } catch (ex: Exception) {
            ActivityMonitor.getInstance(project).registerCustomEvent("Engine.start-docker-daemon", mapOf("error" to ex.message.toString()))
            ActivityMonitor.getInstance(project).registerError(ex, "failed trying to start docker daemon")
            Log.warnWithException(logger, ex, "Failed trying to start docker daemon '{}'", cmd.commandLineString)
        }
    }


    private fun notifyResult(errorMsg: String, resultTask: Consumer<String>) {
        resultTask.accept(errorMsg)
    }

}
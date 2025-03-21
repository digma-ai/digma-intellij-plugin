package org.digma.intellij.plugin.docker

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.execution.util.ExecUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageConstants
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * this service is a front end to docker, it can install,upgrade,stop,start,remove and also provide
 * information about docker installation and running instances.
 * this service is a kind of CRUD service, the core operation of install,upgrade,stop,start,remove
 * should never be called directly because it does not manage single access and locking, meaning if install is called
 * multiple times concurrently it may cause failures. please never call these operations directly, always call
 * LocalInstallationFacade for these CRUD operations,LocalInstallationFacade manages single access and logical flows.
 * information services may be called directly
 */
@Service(Service.Level.APP)
class DockerService {

    private val logger = Logger.getInstance(this::class.java)

    private val engine = Engine()
    private val composeFileProvider = ComposeFileProvider()

    companion object {

        const val NO_DOCKER_COMPOSE_COMMAND = "no docker-compose command"

        @JvmStatic
        fun getInstance(): DockerService {
            return service<DockerService>()
        }
    }


    init {
        migrateDockerComposeFile(composeFileProvider.getComposeFilePath(), logger)
    }


    //this method should not be used to get the file itself, it is mainly for logging and debugging.
    fun getComposeFilePath(): String {
        return composeFileProvider.getComposeFilePath()
    }


    fun isDockerInstalled(): Boolean {
        return isInstalled(DOCKER_COMMAND)
    }

    fun isDockerComposeInstalled(): Boolean {
        return isInstalled(DOCKER_COMPOSE_COMMAND)
    }


    fun collectDigmaContainerLog(): String {
        try {

            if (!isInstalled(DOCKER_COMMAND)) {
                return "could not find docker command"
            }

            val dockerCmd = getDockerCommand()

            val getContainerIdCommand = GeneralCommandLine(dockerCmd, "ps", "--filter", "name=digma-compound", "--format", "{{.ID}}")
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)

            val containerId = ExecUtil.execAndReadLine(getContainerIdCommand)

            val getLogCommand = GeneralCommandLine(dockerCmd, "logs", "--tail", "1000", "$containerId")
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)

            val processOutput: ProcessOutput = ExecUtil.execAndGetOutput(getLogCommand)
            return processOutput.toString()

        } catch (ex: Exception) {
            Log.warnWithException(logger, ex, "Failed in collectDigmaContainerLog")
            ErrorReporter.getInstance().reportError("DockerService.collectDigmaContainerLog", ex)
            return "could not collect docker container log because: $ex"
        }
    }


    @Internal
    fun installEngine(project: Project, resultTask: Consumer<String>) {

        ActivityMonitor.getInstance(project).registerDigmaEngineEventStart("installEngine", mapOf())

        Backgroundable.runInNewBackgroundThread(project, "installing digma engine") {

            var exitValue = ""
            try {

                if (composeFileProvider.ensureComposeFileExists()) {
                    val dockerComposeCmd = getDockerComposeCommand()

                    if (dockerComposeCmd != null) {

                        exitValue = engine.up(project, composeFileProvider.getComposeFile(), dockerComposeCmd)
                        if (exitValue != "0") {
                            ActivityMonitor.getInstance(project).registerDigmaEngineEventError("installEngine", exitValue)
                            Log.log(logger::warn, "error installing engine {}", exitValue)
                            if (isDockerDaemonDownExitValue(exitValue)) {
                                exitValue = doRetryFlowWhenDockerDaemonIsDown(project, exitValue) {
                                    engine.up(project, composeFileProvider.getComposeFile(), dockerComposeCmd)
                                }
                            }
                        }

                        if (exitValue != "0") {
                            ActivityMonitor.getInstance(project).registerDigmaEngineEventError("installEngine", exitValue)
                            Log.log(logger::warn, "error installing engine {}", exitValue)
                        }


                        notifyResult(exitValue, resultTask)
                    } else {
                        ActivityMonitor.getInstance(project).registerDigmaEngineEventError("installEngine", "could not find docker compose command")
                        Log.log(logger::warn, "could not find docker compose command")
                        notifyResult(NO_DOCKER_COMPOSE_COMMAND, resultTask)
                    }
                } else {
                    ActivityMonitor.getInstance(project).registerDigmaEngineEventError("installEngine", "Failed to download docker compose file")
                    Log.log(logger::warn, "Failed to download docker compose file")
                    notifyResult("Failed to download docker compose file", resultTask)
                }

            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError(project, "DockerService.installEngine", e)
                ActivityMonitor.getInstance(project).registerDigmaEngineEventError("installEngine", "Failed in installEngine $e")
                Log.warnWithException(logger, e, "Failed install docker engine {}", e)
                notifyResult("Failed to install docker engine: $e", resultTask)
            } finally {
                ActivityMonitor.getInstance(project).registerDigmaEngineEventEnd(
                    "installEngine", mapOf(
                        "exitValue" to exitValue
                    )
                )
            }
        }
    }

    @Internal
    fun upgradeEngine(project: Project, resultTask: Consumer<String>) {

        ActivityMonitor.getInstance(project).registerDigmaEngineEventStart("upgradeEngine", mapOf())

        Backgroundable.runInNewBackgroundThread(project, "upgrading digma engine") {

            //stop the engine before upgrade using the current compose file, before downloading a new file.
            //the engine should be up otherwise the upgrade would not be triggered.
            //ignore errors, we'll try to upgrade anyway after that.
            try {
                if (composeFileProvider.ensureComposeFileExists()) {
                    val dockerComposeCmd = getDockerComposeCommand()
                    dockerComposeCmd?.let {
                        engine.down(project, composeFileProvider.getComposeFile(), it, false)
                    }
                } else {
                    ActivityMonitor.getInstance(project).registerDigmaEngineEventError(
                        "upgradeEngine",
                        "Failed to stop engine before upgrade because compose file not found"
                    )
                    Log.log(logger::warn, "Failed to download docker compose file")
                }
            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError(project, "DockerService.upgradeEngine", e)
                Log.warnWithException(logger, e, "Failed to stop docker engine {}", e)
            }


            var exitValue = ""
            try {
                if (composeFileProvider.downloadLatestComposeFile()) {
                    val dockerComposeCmd = getDockerComposeCommand()

                    if (dockerComposeCmd != null) {
                        exitValue = engine.up(project, composeFileProvider.getComposeFile(), dockerComposeCmd)
                        //in upgrade there is no need to check if daemon is down because upgrade will not be triggered if the engine is not running.
                        if (exitValue != "0") {
                            ActivityMonitor.getInstance(project).registerDigmaEngineEventError("upgradeEngine", exitValue)
                            Log.log(logger::warn, "error upgrading engine {}", exitValue)
                        }

                        notifyResult(exitValue, resultTask)
                    } else {
                        ActivityMonitor.getInstance(project).registerDigmaEngineEventError("upgradeEngine", "could not find docker compose command")
                        Log.log(logger::warn, "could not find docker compose command")
                        notifyResult(NO_DOCKER_COMPOSE_COMMAND, resultTask)
                    }
                } else {
                    ActivityMonitor.getInstance(project).registerDigmaEngineEventError("upgradeEngine", "Failed to download latest compose file")
                    Log.log(logger::warn, "Failed to download docker compose file")
                    notifyResult("Failed to download docker compose file", resultTask)
                }
            } catch (e: Exception) {
                ErrorReporter.getInstance().reportError(project, "DockerService.upgradeEngine", e)
                ActivityMonitor.getInstance(project).registerDigmaEngineEventError("upgradeEngine", "Failed in upgradeEngine $e")
                Log.warnWithException(logger, e, "Failed install docker engine {}", e)
                notifyResult("Failed to upgrade docker engine: $e", resultTask)
            } finally {
                ActivityMonitor.getInstance(project).registerDigmaEngineEventEnd(
                    "upgradeEngine", mapOf(
                        "exitValue" to exitValue
                    )
                )
            }
        }
    }

    @Internal
    fun stopEngine(project: Project, resultTask: Consumer<String>) {

        ActivityMonitor.getInstance(project).registerDigmaEngineEventStart("stopEngine", mapOf())

        Backgroundable.runInNewBackgroundThread(project, "stopping digma engine") {

            var exitValue = ""
            try {
                if (composeFileProvider.ensureComposeFileExists()) {

                    val dockerComposeCmd = getDockerComposeCommand()

                    if (dockerComposeCmd != null) {

                        exitValue = engine.stop(project, composeFileProvider.getComposeFile(), dockerComposeCmd)
                        if (exitValue != "0") {
                            ActivityMonitor.getInstance(project).registerDigmaEngineEventError("stopEngine", exitValue)
                            Log.log(logger::warn, "error stopping engine {}", exitValue)
                        }
                        notifyResult(exitValue, resultTask)
                    } else {
                        ActivityMonitor.getInstance(project).registerDigmaEngineEventError("stopEngine", "could not find docker compose command")
                        Log.log(logger::warn, "could not find docker compose command")
                        notifyResult(NO_DOCKER_COMPOSE_COMMAND, resultTask)
                    }
                } else {
                    ActivityMonitor.getInstance(project).registerDigmaEngineEventError("stopEngine", "Failed to download docker compose file")
                    Log.log(logger::warn, "Failed to download docker compose file")
                    notifyResult("Failed to download docker compose file", resultTask)
                }
            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError(project, "DockerService.stopEngine", e)
                ActivityMonitor.getInstance(project).registerDigmaEngineEventError("stopEngine", "Failed in stopEngine $e")
                Log.warnWithException(logger, e, "Failed to stop docker engine {}", e)
                notifyResult("Failed to stop docker engine: $e", resultTask)
            } finally {
                ActivityMonitor.getInstance(project).registerDigmaEngineEventEnd(
                    "stopEngine", mapOf(
                        "exitValue" to exitValue
                    )
                )
            }
        }
    }

    @Internal
    fun startEngine(project: Project, resultTask: Consumer<String>) {

        ActivityMonitor.getInstance(project).registerDigmaEngineEventStart("startEngine", mapOf())

        Backgroundable.runInNewBackgroundThread(project, "starting digma engine") {

            var exitValue = ""
            try {
                if (composeFileProvider.ensureComposeFileExists()) {
                    val dockerComposeCmd = getDockerComposeCommand()

                    if (dockerComposeCmd != null) {

                        try {
                            //we try to detect errors when running the docker command. engine.start executes docker-compose up,
                            // if executing docker-compose up while containers exist it will print many errors that are ok but
                            // that interferes with our attempt to detect errors.
                            //so running down and then up solves it.
                            //in any case it's better to stop before start because we don't know the state of the engine, maybe its
                            // partially up and start will fail.
                            engine.down(project, composeFileProvider.getComposeFile(), dockerComposeCmd, false)
                            Thread.sleep(2000)
                        } catch (e: Exception) {
                            ErrorReporter.getInstance().reportError(project, "DockerService.startEngine", e)
                            Log.warnWithException(logger, e, "Failed to stop docker engine {}", e)
                        }

                        exitValue = engine.start(project, composeFileProvider.getComposeFile(), dockerComposeCmd)
                        if (exitValue != "0") {
                            ActivityMonitor.getInstance(project).registerDigmaEngineEventError("startEngine", exitValue)
                            Log.log(logger::warn, "error starting engine {}", exitValue)
                            if (isDockerDaemonDownExitValue(exitValue)) {
                                exitValue = doRetryFlowWhenDockerDaemonIsDown(project, exitValue) {
                                    engine.start(project, composeFileProvider.getComposeFile(), dockerComposeCmd)
                                }
                            }
                        }

                        if (exitValue != "0") {
                            ActivityMonitor.getInstance(project).registerDigmaEngineEventError("startEngine", exitValue)
                            Log.log(logger::warn, "error starting engine {}", exitValue)
                        }

                        notifyResult(exitValue, resultTask)
                    } else {
                        ActivityMonitor.getInstance(project).registerDigmaEngineEventError("startEngine", "could not find docker compose command")
                        Log.log(logger::warn, "could not find docker compose command")
                        notifyResult(NO_DOCKER_COMPOSE_COMMAND, resultTask)
                    }
                } else {
                    ActivityMonitor.getInstance(project).registerDigmaEngineEventError("startEngine", "Failed to download docker compose file")
                    Log.log(logger::warn, "Failed to download docker compose file")
                    notifyResult("Failed to download docker compose file", resultTask)
                }
            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError(project, "DockerService.startEngine", e)
                ActivityMonitor.getInstance(project).registerDigmaEngineEventError("startEngine", "Failed in startEngine $e")
                Log.warnWithException(logger, e, "Failed to start docker engine {}", e)
                notifyResult("Failed to start docker engine: $e", resultTask)
            } finally {
                ActivityMonitor.getInstance(project).registerDigmaEngineEventEnd(
                    "startEngine", mapOf(
                        "exitValue" to exitValue
                    )
                )
            }
        }

    }

    @Internal
    fun removeEngine(project: Project, resultTask: Consumer<String>) {

        ActivityMonitor.getInstance(project).registerDigmaEngineEventStart("removeEngine", mapOf())

        //always mark local engine not installed even if the remove or docker down will fail for some reason,
        // from the plugin perspective local engine is not installed.
        PersistenceService.getInstance().setLocalEngineInstalled(false)

        Backgroundable.runInNewBackgroundThread(project, "uninstalling digma engine") {

            var exitValue = ""
            try {
                if (composeFileProvider.ensureComposeFileExists()) {
                    val dockerComposeCmd = getDockerComposeCommand()

                    if (dockerComposeCmd != null) {
                        exitValue = engine.remove(project, composeFileProvider.getComposeFile(), dockerComposeCmd)
                        if (exitValue != "0") {
                            ActivityMonitor.getInstance(project).registerDigmaEngineEventError("removeEngine", exitValue)
                            Log.log(logger::warn, "error uninstalling engine {}", exitValue)
                        }
                        notifyResult(exitValue, resultTask)
                    } else {
                        ActivityMonitor.getInstance(project).registerDigmaEngineEventError("removeEngine", "could not find docker compose command")
                        Log.log(logger::warn, "could not find docker compose command")
                        notifyResult(NO_DOCKER_COMPOSE_COMMAND, resultTask)
                    }

                    try {
                        //always delete file here, it's an uninstallation
                        composeFileProvider.deleteFile()
                    } catch (e: Exception) {
                        Log.log(logger::warn, "Failed to delete compose file")
                        ActivityMonitor.getInstance(project).registerDigmaEngineEventError("removeEngine", "failed to delete compose file: $e")
                    }

                } else {
                    ActivityMonitor.getInstance(project).registerDigmaEngineEventError("removeEngine", "Failed to download docker compose file")
                    Log.log(logger::warn, "Failed to download docker compose file")
                    notifyResult("Failed to download docker compose file", resultTask)
                }
            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError(project, "DockerService.removeEngine", e)
                ActivityMonitor.getInstance(project).registerDigmaEngineEventError("removeEngine", "failed in removeEngine $e")
                Log.warnWithException(logger, e, "Failed to remove docker engine {}", e)
                notifyResult("Failed to remove docker engine: $e", resultTask)
            } finally {
                ActivityMonitor.getInstance(project).registerDigmaEngineEventEnd(
                    "removeEngine", mapOf(
                        "exitValue" to exitValue
                    )
                )
            }
        }
    }


    private fun isDockerDaemonDownExitValue(exitValue: String): Boolean {
        return exitValue.contains("Cannot connect to the Docker daemon", true) ||//mac, linux
                exitValue.contains("docker daemon is not running", true) || //win
                //this is an error on windows with docker desktop that will be solved by starting docker desktop
                (exitValue.contains("error during connect", true) && exitValue.contains("The system cannot find the file specified", true)) || //win
                exitValue.contains("Error while fetching server API version", true)
    }


    private fun doRetryFlowWhenDockerDaemonIsDown(project: Project, prevExitValue: String, runCommand: Supplier<String>): String {

        val eventName = "docker-daemon-is-down"

        ActivityMonitor.getInstance(project).registerDigmaEngineEventInfo(eventName, mapOf("exitValue" to prevExitValue))

        //try to start docker daemon, usually it will fail
        tryStartDockerDaemon(project)

        //run the command again, maybe restart docker daemon succeeded
        var exitValue = runCommand.get()

        if (isDockerDaemonDownExitValue(exitValue)) {
            var res = MessageConstants.YES
            ApplicationManager.getApplication().invokeAndWait {
                res = Messages.showYesNoDialog(
                    project,
                    "It seems that docker daemon is not running.\n" +
                            "Please make sure the Docker daemon is running by restarting the service or starting docker desktop app.\n" +
                            "Once the Docker daemon is running, press the retry button.\n",
                    "Digma Engine Failed to Start",
                    "Retry",
                    "Cancel",
                    AllIcons.General.Information
                )
            }
            if (res == MessageConstants.YES) {
                ActivityMonitor.getInstance(project).registerDigmaEngineEventInfo(eventName, mapOf("message" to "retry triggered by user"))
                exitValue = runCommand.get()
                if (isDockerDaemonDownExitValue(exitValue)) {
                    ActivityMonitor.getInstance(project)
                        .registerDigmaEngineEventInfo(eventName, mapOf("message" to "restart daemon failed after user retry attempt"))
                    ApplicationManager.getApplication().invokeAndWait {
                        Messages.showMessageDialog(
                            project,
                            "Digma engine failed to start\nDocker daemon is down",
                            "Digma Engine Error",
                            AllIcons.General.Error
                        )
                    }
                } else {
                    ActivityMonitor.getInstance(project)
                        .registerDigmaEngineEventInfo(eventName, mapOf("message" to "restart daemon success after user retry attempt"))
                }
            } else {
                ActivityMonitor.getInstance(project).registerDigmaEngineEventInfo(eventName, mapOf("message" to "retry canceled by user"))
            }
        }


        return exitValue
    }


    private fun tryStartDockerDaemon(project: Project) {

        Log.log(logger::info, "Trying to start docker daemon")

        val command: List<List<String>> = if (SystemInfo.isMac) {
            listOf(listOf("docker-machine", "restart"))
        } else if (SystemInfo.isWindows) {
            listOf(listOf("wsl.exe", "-u", "root", "-e", "sh", "-c", "service docker status || service docker start"))
        } else if (SystemInfo.isLinux) {
            listOf(
                listOf("systemctl", "start", "docker.service"),
                listOf("sudo", "systemctl", "start", "docker.service")
            )
        } else {
            listOf(listOf("docker", "start")) //just a useless fallback to satisfy kotlin if/else
        }


        for (cmd in command) {

            val generalCommandLine = GeneralCommandLine(cmd)
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)

            val eventName = "start-docker-daemon"
            try {

                ActivityMonitor.getInstance(project).registerDigmaEngineEventStart(eventName, mapOf("command" to cmd))

                Log.log(logger::info, "executing command: {}", generalCommandLine.commandLineString)
                val processOutput = ExecUtil.execAndGetOutput(generalCommandLine, 10000)
                val output = "exitCode:${processOutput.exitCode}, stdout:${processOutput.stdout}, stderr:${processOutput.stderr}"

                ActivityMonitor.getInstance(project).registerDigmaEngineEventInfo(eventName, mapOf("result" to output))
                Log.log(logger::info, "start docker command result: {}", output)

                if (processOutput.exitCode == 0) {
                    break
                }

            } catch (ex: Exception) {
                ActivityMonitor.getInstance(project).registerDigmaEngineEventError(eventName, ex.message.toString())
                ErrorReporter.getInstance().reportError(project, "DockerService.tryStartDockerDaemon", ex)
                Log.warnWithException(logger, ex, "Failed trying to start docker daemon '{}'", generalCommandLine.commandLineString)
            } finally {
                ActivityMonitor.getInstance(project).registerDigmaEngineEventEnd(eventName, mapOf("command" to cmd))
            }
        }
    }


    private fun notifyResult(errorMsg: String, resultTask: Consumer<String>) {
        resultTask.accept(errorMsg)
    }


}
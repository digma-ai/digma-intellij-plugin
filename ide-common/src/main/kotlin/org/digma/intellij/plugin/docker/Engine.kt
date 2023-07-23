package org.digma.intellij.plugin.docker

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

internal class Engine {

    private val logger = Logger.getInstance(this::class.java)

    private val streamExecutor = Executors.newFixedThreadPool(2)

    private val engineLock = ReentrantLock()


    fun up(project: Project, composeFile: Path, dockerComposeCmd: List<String>): String {

        Log.log(logger::info, "starting docker compose up")

        val processBuilder = GeneralCommandLine(dockerComposeCmd)
            .withParameters("-f", composeFile.toString(), "up", "-d")
            .withWorkDirectory(composeFile.toFile().parentFile)
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            .withRedirectErrorStream(true)
            .toProcessBuilder()

        return executeCommand(project, "up", composeFile, processBuilder)
    }


    fun down(project: Project, composeFile: Path, dockerComposeCmd: List<String>, reportToPosthog: Boolean = true): String {

        Log.log(logger::info, "starting docker compose down")

        val processBuilder = GeneralCommandLine(dockerComposeCmd)
            .withParameters("-f", composeFile.toString(), "down")
            .withWorkDirectory(composeFile.toFile().parentFile)
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            .withRedirectErrorStream(true)
            .toProcessBuilder()

        return executeCommand(project, "down", composeFile, processBuilder, reportToPosthog)

    }


    fun start(project: Project, composeFile: Path, dockerComposeCmd: List<String>): String {

        Log.log(logger::info, "starting docker compose start")

        val processBuilder = GeneralCommandLine(dockerComposeCmd)
            .withParameters("-f", composeFile.toString(), "up", "-d")
            .withWorkDirectory(composeFile.toFile().parentFile)
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            .withRedirectErrorStream(true)
            .toProcessBuilder()

        return executeCommand(project, "up", composeFile, processBuilder)

    }


    fun stop(project: Project, composeFile: Path, dockerComposeCmd: List<String>): String {

        Log.log(logger::info, "starting docker compose stop")

        val processBuilder = GeneralCommandLine(dockerComposeCmd)
            .withParameters("-f", composeFile.toString(), "stop")
            .withWorkDirectory(composeFile.toFile().parentFile)
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            .withRedirectErrorStream(true)
            .toProcessBuilder()

        return executeCommand(project, "stop", composeFile, processBuilder)

    }


    fun remove(project: Project, composeFile: Path, dockerComposeCmd: List<String>): String {

        Log.log(logger::info, "starting uninstall")

        //to remove images use parameters
        //"-f", composeFile.toString(), "down", "--rmi", "all", "-v", "--remove-orphans"

        val processBuilder = GeneralCommandLine(dockerComposeCmd)
            .withParameters("-f", composeFile.toString(), "down")
            .withWorkDirectory(composeFile.toFile().parentFile)
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            .withRedirectErrorStream(true)
            .toProcessBuilder()

        return executeCommand(project, "down", composeFile, processBuilder)

    }


    private fun executeCommand(
        project: Project,
        name: String,
        composeFile: Path,
        processBuilder: ProcessBuilder,
        reportToPosthog: Boolean = true,
    ): String {

        try {
            engineLock.lock()

            Log.log(logger::info, "executing {}, compose file {}, command {}", name, composeFile, processBuilder.command())

            if (reportToPosthog) {
                ActivityMonitor.getInstance(project).registerDigmaEngineEventStart(
                    name, mapOf()
                )
            }

            val errorMessages = mutableListOf<String>()
            val allOutputLines = mutableListOf<String>()


            val process = processBuilder.start()

            Log.log(logger::info, "started process {}", process.info())

            streamExecutor.submit(StreamGobbler(process.inputStream) {
                collectErrors(it, errorMessages)
                collectAll(it, allOutputLines)
                Log.log(logger::info, "DigmaDocker: $it")
            })
            streamExecutor.submit(StreamGobbler(process.errorStream) {
                collectErrors(it, errorMessages)
                collectAll(it, allOutputLines)
                Log.log(logger::info, "DigmaDockerError: $it")
            })

            val success = process.waitFor(10, TimeUnit.MINUTES)

            val exitCode = process.exitValue()

            if (success) {
                Log.log(
                    logger::info,
                    "docker compose completed successfully exit code: [{}], command: [{}], process info: [{}]",
                    exitCode,
                    processBuilder.command(),
                    process.info()
                )
            } else {
                Log.log(
                    logger::info,
                    "docker compose unsuccessful exit code: [{}], command: [{}], process info: [{}]",
                    exitCode,
                    processBuilder.command(),
                    process.info()
                )
            }

            val exitValue = buildExitValue(exitCode, success, errorMessages, allOutputLines)

            if (reportToPosthog) {
                ActivityMonitor.getInstance(project).registerDigmaEngineEventEnd(
                    name, mapOf(
                        "exitValue" to exitValue
                    )
                )
            }

            return exitValue

        } catch (e: Exception) {
            val stringWriter = StringWriter()
            e.printStackTrace(PrintWriter(stringWriter))
            if (reportToPosthog) {
                ActivityMonitor.getInstance(project).registerDigmaEngineEventError(name, stringWriter.toString())
            }
            Log.warnWithException(logger, e, "error running docker command {}", processBuilder.command())
            return e.message ?: e.toString()
        } finally {
            if (engineLock.isHeldByCurrentThread) {
                engineLock.unlock()
            }
        }
    }


    private fun buildExitValue(exitValue: Int, success: Boolean, errorMessages: List<String>, allOutputLines: MutableList<String>): String {

        if (!success || exitValue != 0) {
            var exitMessage = "process failed with code $exitValue"
            if (errorMessages.isNotEmpty()) {
                exitMessage = exitMessage.plus(":").plus(buildErrorMessages(errorMessages))
            }

            if (allOutputLines.isNotEmpty()) {
                exitMessage = exitMessage.plus(":").plus(System.lineSeparator()).plus(buildOutputMessages(allOutputLines))
            }

            return exitMessage
        }

        if (errorMessages.isNotEmpty()) {
            return "process succeeded but there are error messages:".plus(buildErrorMessages(errorMessages))
        }

        return "0"
    }

    private fun buildErrorMessages(errorMessages: List<String>): String {

        var message = ""

        errorMessages.forEach {
            message = message.plus(it).plus(",")
        }
        return message
    }

    private fun buildOutputMessages(errorMessages: List<String>): String {

        var message = ""

        errorMessages.forEach {
            message = message.plus(it).plus(System.lineSeparator())
        }
        return message
    }


    //best effort to collect error codes
    private fun collectErrors(line: String, errorMessages: MutableList<String>) {

        try {
            if (line.trim().startsWith("Error ") || line.trim().startsWith("Error: ")) {
                errorMessages.add(line)
            }
        } catch (e: Exception) {
            //ignore
        }
    }

    private fun collectAll(line: String, allLines: MutableList<String>) {
        allLines.add(line)
    }

}
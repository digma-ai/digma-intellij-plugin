package org.digma.intellij.plugin.docker

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

internal class Engine {

    private val logger = Logger.getInstance(this::class.java)

    private val streamExecutor = Executors.newFixedThreadPool(2)

    //executeCommandLock should be used only in method executeCommand which is not recursive, other methods here may call each other
    // especially if there is doBetweenRetries in executeCommandWithRetry
    private val executeCommandLock = ReentrantLock(true)

    //this message is used to identify timeout of the process
    private val timeoutMessage = "process exited with timeout"
    private val timeoutExitCode: Int = -999


    fun up(project: Project, composeFile: File, dockerComposeCmd: List<String>): String {

        Log.log(logger::info, "starting docker compose up")

        val processBuilder = GeneralCommandLine(dockerComposeCmd)
            .withParameters("-f", composeFile.canonicalPath.toString(), "up", "-d", "--remove-orphans")
            .withWorkDirectory(composeFile.parentFile)
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            .withRedirectErrorStream(true)
            .toProcessBuilder()

        //if up failed do down before retrying, in many cases it will fix the problem
        val actionBetweenRetry = Runnable {
            remove(project, composeFile, dockerComposeCmd, reportToPosthog = false)
        }


        return executeCommandWithRetry(
            project,
            "up",
            composeFile,
            processBuilder,
            reportToPosthog = true,
            ignoreNonRealErrors = true,
            doBetweenRetries = actionBetweenRetry
        )
    }


    fun down(project: Project, composeFile: File, dockerComposeCmd: List<String>, reportToPosthog: Boolean = true): String {

        Log.log(logger::info, "starting docker compose down")

        val processBuilder = GeneralCommandLine(dockerComposeCmd)
            .withParameters("-f", composeFile.canonicalPath.toString(), "down")
            .withWorkDirectory(composeFile.parentFile)
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            .withRedirectErrorStream(true)
            .toProcessBuilder()

        return executeCommandWithRetry(project, "down", composeFile, processBuilder, reportToPosthog)

    }


    fun start(project: Project, composeFile: File, dockerComposeCmd: List<String>): String {

        Log.log(logger::info, "starting docker compose start")

        val processBuilder = GeneralCommandLine(dockerComposeCmd)
            .withParameters("-f", composeFile.canonicalPath.toString(), "up", "-d")
            .withWorkDirectory(composeFile.parentFile)
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            .withRedirectErrorStream(true)
            .toProcessBuilder()

        //if up failed do down before retrying, in many cases it will fix the problem
        val actionBetweenRetry = Runnable {
            remove(project, composeFile, dockerComposeCmd, reportToPosthog = false)
        }

        return executeCommandWithRetry(project, "up", composeFile, processBuilder, doBetweenRetries = actionBetweenRetry)

    }


    fun stop(project: Project, composeFile: File, dockerComposeCmd: List<String>): String {

        Log.log(logger::info, "starting docker compose stop")

        val processBuilder = GeneralCommandLine(dockerComposeCmd)
            .withParameters("-f", composeFile.canonicalPath.toString(), "stop")
            .withWorkDirectory(composeFile.parentFile)
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            .withRedirectErrorStream(true)
            .toProcessBuilder()

        return executeCommandWithRetry(project, "stop", composeFile, processBuilder)

    }


    fun remove(project: Project, composeFile: File, dockerComposeCmd: List<String>, reportToPosthog: Boolean = true): String {

        Log.log(logger::info, "starting uninstall")

        //to remove images use parameters
        //"-f", composeFile.toString(), "down", "--rmi", "all", "-v", "--remove-orphans"

        val processBuilder = GeneralCommandLine(dockerComposeCmd)
            .withParameters("-f", composeFile.canonicalPath.toString(), "down")
            .withWorkDirectory(composeFile.parentFile)
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            .withRedirectErrorStream(true)
            .toProcessBuilder()

        return executeCommandWithRetry(project, "down", composeFile, processBuilder, reportToPosthog = reportToPosthog)

    }


    private fun executeCommandWithRetry(
        project: Project,
        name: String,
        composeFile: File,
        processBuilder: ProcessBuilder,
        reportToPosthog: Boolean = true,
        ignoreNonRealErrors: Boolean = false,
        doBetweenRetries: Runnable? = null
    ): String {

        //try 3 times in case of failure
        repeat(3) { count ->
            Log.log(logger::info, "executing command {}, attempt {}", name, count)
            val exitValue = executeCommand(project, name, composeFile, processBuilder, reportToPosthog, ignoreNonRealErrors)
            if (shouldExit(exitValue)) {
                Log.log(logger::info, "docker command {} completed after retry {} with exit value {}", name, count, exitValue)
                return exitValue
            }

            if (reportToPosthog) {
                ActivityMonitor.getInstance(project).registerDigmaEngineEventRetry(
                    name, mapOf(
                        "exitValue" to exitValue,
                        "retry" to count
                    )
                )
            }

            Log.log(logger::info, "docker command {} failed with exit value {}, retrying..", name, exitValue)

            doBetweenRetries?.let {
                Log.log(logger::info, "executing doBetweenRetries action before next retry")
                it.run()
            }
        }

        //last chance
        Log.log(logger::info, "executing command {}, last chance after 3 failures", name)
        return executeCommand(project, name, composeFile, processBuilder)
    }

    private fun shouldExit(exitValue: String): Boolean {
        return !shouldRetry(exitValue)
    }

    private fun shouldRetry(exitValue: String): Boolean {
        //add here more evaluations in exit value that should trigger a retry
        return exitValue != "0" && isRetryTriggerExitValue(exitValue)

    }

    private fun isRetryTriggerExitValue(exitValue: String): Boolean {

        //"process exited with timeout" is the message set in buildExitValue
        return exitValue.startsWith(timeoutMessage, true) ||
                exitValue.contains("unexpected EOF", true) ||
                exitValue.contains("pod already exists", true)

    }


    private fun executeCommand(
        project: Project,
        name: String,
        composeFile: File,
        processBuilder: ProcessBuilder,
        reportToPosthog: Boolean = true,
        ignoreNonRealErrors: Boolean = false,
    ): String {

        try {
            executeCommandLock.lock()

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
                collectErrors(it, errorMessages, ignoreNonRealErrors)
                collectAll(it, allOutputLines)
                Log.log(logger::info, "DigmaDocker: $it")
            })
            streamExecutor.submit(StreamGobbler(process.errorStream) {
                collectErrors(it, errorMessages, ignoreNonRealErrors)
                collectAll(it, allOutputLines)
                Log.log(logger::info, "DigmaDockerError: $it")
            })


            val timeout = 15L
            var success = try {
                process.waitFor(timeout, TimeUnit.MINUTES)
            } catch (_: InterruptedException) {
                false
            }

            val exitCode = try {
                process.exitValue()
            } catch (e: IllegalThreadStateException) {
                Log.warnWithException(logger, e, "process has not exited after {} minutes", timeout)
                ActivityMonitor.getInstance(project).registerDigmaEngineEventError(name, "process did not exit after $timeout minutes")
                process.destroyForcibly()

                //wait one more minute for the process to terminate, if it doesn't terminate then we have a problem,
                // and it will probably be a zombie
                try {
                    process.waitFor(1, TimeUnit.MINUTES)
                } catch (_: InterruptedException) { /*ignore*/
                }

                success = false
                timeoutExitCode
            }


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
            if (executeCommandLock.isHeldByCurrentThread) {
                executeCommandLock.unlock()
            }
        }
    }


    private fun buildExitValue(exitValue: Int, success: Boolean, errorMessages: List<String>, allOutputLines: MutableList<String>): String {

        if (!success || exitValue != 0) {
            var exitMessage = if (!success || exitValue == timeoutExitCode) {
                "$timeoutMessage and exit code $exitValue"
            } else {
                "process failed with code $exitValue"
            }
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
    private fun collectErrors(line: String, errorMessages: MutableList<String>, ignoreNonRealErrors: Boolean) {

        try {
            if (line.trim().startsWith("Error ") || line.trim().startsWith("Error: ")) {

                if (ignoreNonRealErrors && isNonRealError(line)) {
                    return
                }

                errorMessages.add(line)
            }
        } catch (e: Exception) {
            //ignore
        }
    }

    private fun isNonRealError(line: String): Boolean {
        //some error messages that are not real errors.
        //for example podman will print "no such volume" on new installation for every new volume
        return line.lowercase().contains("no such volume")
    }

    private fun collectAll(line: String, allLines: MutableList<String>) {
        allLines.add(line)
    }

}
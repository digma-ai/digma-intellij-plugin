package org.digma.intellij.plugin.docker

import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.log.Log
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class Engine {

    private val logger = Logger.getInstance(this::class.java)

    private val streamExecutor = Executors.newFixedThreadPool(2)


    fun up(composeFile: Path, dockerComposeCmd: List<String>): String {

        Log.log(logger::info, "starting docker compose up")

        val processBuilder = ProcessBuilder()
        if (dockerComposeCmd.size == 1) {
            processBuilder.command(dockerComposeCmd[0], "-f", composeFile.toString(), "up", "-d")
        } else {
            processBuilder.command(dockerComposeCmd[0], dockerComposeCmd[1], "-f", composeFile.toString(), "up", "-d")
        }

        return executeCommand("up", composeFile, processBuilder)
    }


    fun down(composeFile: Path, dockerComposeCmd: List<String>): String {

        Log.log(logger::info, "starting docker compose down")

        val processBuilder = ProcessBuilder()
        if (dockerComposeCmd.size == 1) {
            processBuilder.command(dockerComposeCmd[0], "-f", composeFile.toString(), "down")
        } else {
            processBuilder.command(dockerComposeCmd[0], dockerComposeCmd[1], "-f", composeFile.toString(), "down")
        }

        return executeCommand("down", composeFile, processBuilder)

    }


    fun start(composeFile: Path, dockerComposeCmd: List<String>): String {

        Log.log(logger::info, "starting docker compose start")

        val processBuilder = ProcessBuilder()
        if (dockerComposeCmd.size == 1) {
            processBuilder.command(dockerComposeCmd[0], "-f", composeFile.toString(), "up", "-d")
        } else {
            processBuilder.command(dockerComposeCmd[0], dockerComposeCmd[1], "-f", composeFile.toString(), "up", "-d")
        }

        return executeCommand("start", composeFile, processBuilder)

    }


    fun stop(composeFile: Path, dockerComposeCmd: List<String>): String {

        Log.log(logger::info, "starting docker compose stop")

        val processBuilder = ProcessBuilder()
        if (dockerComposeCmd.size == 1) {
            processBuilder.command(dockerComposeCmd[0], "-f", composeFile.toString(), "stop")
        } else {
            processBuilder.command(dockerComposeCmd[0], dockerComposeCmd[1], "-f", composeFile.toString(), "stop")
        }

        return executeCommand("stop", composeFile, processBuilder)

    }


    fun remove(composeFile: Path, dockerComposeCmd: List<String>): String {

        Log.log(logger::info, "starting uninstall")

        val processBuilder = ProcessBuilder()
        if (dockerComposeCmd.size == 1) {
            processBuilder.command(dockerComposeCmd[0], "-f", composeFile.toString(), "down")
        } else {
            processBuilder.command(dockerComposeCmd[0], dockerComposeCmd[1], "-f", composeFile.toString(), "down")
        }
//            if (dockerComposeCmd.size == 1) {
//                processBuilder.command(dockerComposeCmd[0], "-f", composeFile.toString(), "down", "--rmi", "all", "-v", "--remove-orphans")
//            } else {
//                processBuilder.command(dockerComposeCmd[0],dockerComposeCmd[1],"-f",composeFile.toString(),"down","--rmi","all","-v","--remove-orphans")
//            }

        return executeCommand("down", composeFile, processBuilder)

    }


    private fun executeCommand(name: String, composeFile: Path, processBuilder: ProcessBuilder): String {

        Log.log(logger::info, "executing {}, command {}", name, processBuilder.command())

        val errorCodes = mutableSetOf<Int>()

        try {

            processBuilder.directory(composeFile.toFile().parentFile)
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            Log.log(logger::info, "started process {}", process.info())

            streamExecutor.submit(StreamGobbler(process.inputStream) {
                collectErrorCode(it, errorCodes)
                Log.log(logger::info, "DigmaDocker: $it")
            })
            streamExecutor.submit(StreamGobbler(process.errorStream) {
                collectErrorCode(it, errorCodes)
                Log.log(logger::info, "DigmaDockerError: $it")
            })

            val success = process.waitFor(10, TimeUnit.MINUTES)

            val exitValue = process.exitValue()

            if (success) {
                Log.log(
                    logger::info,
                    "docker compose completed successfully exit code: [{}], command: [{}], process info: [{}]",
                    exitValue,
                    processBuilder.command(),
                    process.info()
                )
            } else {
                Log.log(
                    logger::info,
                    "docker compose unsuccessful exit code: [{}], command: [{}], process info: [{}]",
                    exitValue,
                    processBuilder.command(),
                    process.info()
                )
            }

            return buildExitValue(exitValue, success, errorCodes)

        } catch (e: Exception) {
            Log.warnWithException(logger, e, "error running docker command {}", processBuilder.command())
            return e.message ?: e.toString()
        }
    }


    private fun buildExitValue(exitValue: Int, success: Boolean, errorCodes: Set<Int>): String {

        if (!success || exitValue != 0) {
            var exitMessage = "process failed with code $exitValue"
            if (errorCodes.isNotEmpty()) {
                exitMessage = exitMessage.plus(":").plus(System.lineSeparator()).plus(buildErrorMessagesFromCodes(errorCodes))
            }
            return exitMessage
        }

        if (errorCodes.isNotEmpty()) {
            return "process succeeded but some containers failed:".plus(System.lineSeparator()).plus(buildErrorMessagesFromCodes(errorCodes))
        }

        return "0"
    }

    private fun buildErrorMessagesFromCodes(errorCodes: Set<Int>): String {

        var message = ""

        errorCodes.forEach {
            message = message.plus("[$it] ")
            when (it) {
                1 -> message = message.plus("Application error").plus(System.lineSeparator())
                125 -> message = message.plus("Container failed to run error").plus(System.lineSeparator())
                126 -> message = message.plus("Command invoke error").plus(System.lineSeparator())
                127 -> message = message.plus("File or directory not found").plus(System.lineSeparator())
                128 -> message = message.plus("Invalid argument used on exit").plus(System.lineSeparator())
                134 -> message = message.plus("Abnormal termination").plus(System.lineSeparator())
                137 -> message = message.plus("Immediate termination").plus(System.lineSeparator())
                139 -> message = message.plus("Segmentation fault").plus(System.lineSeparator())
                143 -> message = message.plus("Graceful termination").plus(System.lineSeparator())
                255 -> message = message.plus("Exit Status Out Of Range").plus(System.lineSeparator())
            }
        }
        return message
    }

    /*
    Exit Code 1	Application error	Container was stopped due to application error or incorrect reference in the image specification
    Exit Code 125	Container failed to run error	The docker run command did not execute successfully
    Exit Code 126	Command invoke error	A command specified in the image specification could not be invoked
    Exit Code 127	File or directory not found	File or directory specified in the image specification was not found
    Exit Code 128	Invalid argument used on exit	Exit was triggered with an invalid exit code (valid codes are integers between 0-255)
    Exit Code 134	Abnormal termination (SIGABRT)	The container aborted itself using the abort() function.
    Exit Code 137	Immediate termination (SIGKILL)	Container was immediately terminated by the operating system via SIGKILL signal
    Exit Code 139	Segmentation fault (SIGSEGV)	Container attempted to access memory that was not assigned to it and was terminated
    Exit Code 143	Graceful termination (SIGTERM)	Container received warning that it was about to be terminated, then terminated
    Exit Code 255	Exit Status Out Of Range	Container exited, returning an exit code outside the acceptable range, meaning the cause of the error is not known
     */


    //best effort to collect error codes
    private fun collectErrorCode(line: String, errorCodes: MutableSet<Int>) {

        try {
            if (line.contains("exit code")) {
                val code = line.substringAfterLast(" ").toInt()
                if (code != 0) {
                    errorCodes.add(code)
                }
            }
        } catch (e: Exception) {
            //ignore
        }
    }


}
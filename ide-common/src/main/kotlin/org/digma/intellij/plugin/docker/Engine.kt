package org.digma.intellij.plugin.docker

import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.log.Log
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

internal class Engine {

    private val logger = Logger.getInstance(this::class.java)

    private val streamExecutor = Executors.newFixedThreadPool(2)

    private val engineLock = ReentrantLock()


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

        val errorMessages = mutableListOf<String>()

        try {
            engineLock.lock()

            processBuilder.directory(composeFile.toFile().parentFile)
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            Log.log(logger::info, "started process {}", process.info())

            streamExecutor.submit(StreamGobbler(process.inputStream) {
                collectErrors(it, errorMessages)
                Log.log(logger::info, "DigmaDocker: $it")
            })
            streamExecutor.submit(StreamGobbler(process.errorStream) {
                collectErrors(it, errorMessages)
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

            return buildExitValue(exitValue, success, errorMessages)

        } catch (e: Exception) {
            Log.warnWithException(logger, e, "error running docker command {}", processBuilder.command())
            return e.message ?: e.toString()
        } finally {
            if (engineLock.isHeldByCurrentThread) {
                engineLock.unlock()
            }
        }
    }


    private fun buildExitValue(exitValue: Int, success: Boolean, errorMessages: List<String>): String {

        if (!success || exitValue != 0) {
            var exitMessage = "process failed with code $exitValue"
            if (errorMessages.isNotEmpty()) {
                exitMessage = exitMessage.plus(":").plus(System.lineSeparator()).plus(buildErrorMessages(errorMessages))
            }
            return exitMessage
        }

        if (errorMessages.isNotEmpty()) {
            return "process succeeded but some containers failed:".plus(System.lineSeparator()).plus(buildErrorMessages(errorMessages))
        }

        return "0"
    }

    private fun buildErrorMessages(errorMessages: List<String>): String {

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


}
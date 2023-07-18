package org.digma.intellij.plugin.docker

import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.log.Log
import org.jsoup.helper.Consumer
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class Engine {

    private val logger = Logger.getInstance(this::class.java)

    private val streamExecutor = Executors.newFixedThreadPool(2)


    fun up(composeFile: Path, dockerComposeCmd: String): Boolean {

        try {

            Log.log(logger::info, "starting installation")

//            val process = GeneralCommandLine(dockerComposeCmd, "-f", composeFile.toString(), "up", "-d")
//                .withWorkDirectory(composeFile.toFile().parent)
//                .createProcess()

            val processBuilder = ProcessBuilder()
            processBuilder.command(dockerComposeCmd, "-f", composeFile.toString(), "up", "-d")
            processBuilder.directory(composeFile.toFile().parentFile)
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            Log.log(logger::info, "started process {}", process.info())

            streamExecutor.submit(StreamGobbler(process.inputStream, Consumer {
                Log.log(logger::info, "DigmaDocker: $it")
            }))
            streamExecutor.submit(StreamGobbler(process.errorStream, Consumer {
                Log.log(logger::info, "DigmaDockerError: $it")
            }))

            val success = process.waitFor(10, TimeUnit.MINUTES)

            val exitValue = process.exitValue()

            if (success) {
                Log.log(logger::info, "installation completed successfully [exit code {}] for {}", exitValue, process.info())
            } else {
                Log.log(logger::info, "installation unsuccessful [exit code {}] for {}", exitValue, process.info())
            }

            return success

        } catch (e: Exception) {
            Log.warnWithException(logger, e, "error installing engined")
        }
        return false
    }


    fun down(composeFile: Path, dockerComposeCmd: String): Boolean {
        try {

            Log.log(logger::info, "starting shutdown")

//            val process = GeneralCommandLine(dockerComposeCmd, "-f", composeFile.toString(), "up", "-d")
//                .withWorkDirectory(composeFile.toFile().parent)
//                .createProcess()

            val processBuilder = ProcessBuilder()
            processBuilder.command(dockerComposeCmd, "-f", composeFile.toString(), "down")
            processBuilder.directory(composeFile.toFile().parentFile)
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            Log.log(logger::info, "started process {}", process.info())

            streamExecutor.submit(StreamGobbler(process.inputStream, Consumer {
                Log.log(logger::info, "DigmaDocker: $it")
            }))
            streamExecutor.submit(StreamGobbler(process.errorStream, Consumer {
                Log.log(logger::info, "DigmaDockerError: $it")
            }))

            val success = process.waitFor(5, TimeUnit.MINUTES)

            val exitValue = process.exitValue()

            if (success) {
                Log.log(logger::info, "shutdown completed successfully [exit code {}] for {}", exitValue, process.info())
            } else {
                Log.log(logger::info, "shutdown unsuccessful [exit code {}] for {}", exitValue, process.info())
            }

            return success

        } catch (e: Exception) {
            Log.warnWithException(logger, e, "error shutdown engine")
        }
        return false
    }


    fun remove(composeFile: Path, dockerComposeCmd: String): Boolean {
        try {

            Log.log(logger::info, "starting uninstall")

//            val process = GeneralCommandLine(dockerComposeCmd, "-f", composeFile.toString(), "up", "-d")
//                .withWorkDirectory(composeFile.toFile().parent)
//                .createProcess()

            val processBuilder = ProcessBuilder()
            processBuilder.command(dockerComposeCmd, "-f", composeFile.toString(), "down", "--rmi", "all", "-v", "--remove-orphans")
            processBuilder.directory(composeFile.toFile().parentFile)
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            Log.log(logger::info, "started process {}", process.info())

            streamExecutor.submit(StreamGobbler(process.inputStream, Consumer {
                Log.log(logger::info, "DigmaDocker: $it")
            }))
            streamExecutor.submit(StreamGobbler(process.errorStream, Consumer {
                Log.log(logger::info, "DigmaDockerError: $it")
            }))

            val success = process.waitFor(5, TimeUnit.MINUTES)

            val exitValue = process.exitValue()

            if (success) {
                Log.log(logger::info, "uninstall completed successfully [exit code {}] for {}", exitValue, process.info())
            } else {
                Log.log(logger::info, "uninstall unsuccessful [exit code {}] for {}", exitValue, process.info())
            }

            return success

        } catch (e: Exception) {
            Log.warnWithException(logger, e, "error uninstalling engine")
        }
        return false
    }


}
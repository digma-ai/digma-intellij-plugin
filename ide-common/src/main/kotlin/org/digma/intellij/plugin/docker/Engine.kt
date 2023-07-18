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

        try {

            Log.log(logger::info, "starting docker compose")

            val processBuilder = ProcessBuilder()
            if (dockerComposeCmd.size == 1) {
                processBuilder.command(dockerComposeCmd[0], "-f", composeFile.toString(), "up", "-d")
            } else {
                processBuilder.command(dockerComposeCmd[0], dockerComposeCmd[1], "-f", composeFile.toString(), "up", "-d")
            }
            processBuilder.directory(composeFile.toFile().parentFile)
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            Log.log(logger::info, "started process {}", process.info())

            streamExecutor.submit(StreamGobbler(process.inputStream) {
                Log.log(logger::info, "DigmaDocker: $it")
            })
            streamExecutor.submit(StreamGobbler(process.errorStream) {
                Log.log(logger::info, "DigmaDockerError: $it")
            })

            val success = process.waitFor(10, TimeUnit.MINUTES)

            val exitValue = process.exitValue()

            if (success) {
                Log.log(logger::info, "docker compose completed successfully [exit code {}] for {}", exitValue, process.info())
            } else {
                Log.log(logger::info, "docker compose unsuccessful [exit code {}] for {}", exitValue, process.info())
            }

            return exitValue.toString()

        } catch (e: Exception) {
            Log.warnWithException(logger, e, "error starting engined")
            return e.message ?: e.toString()
        }
    }


    fun down(composeFile: Path, dockerComposeCmd: List<String>): String {
        try {

            Log.log(logger::info, "starting docker compose down")

            val processBuilder = ProcessBuilder()
            if (dockerComposeCmd.size == 1) {
                processBuilder.command(dockerComposeCmd[0], "-f", composeFile.toString(), "down")
            } else {
                processBuilder.command(dockerComposeCmd[0], dockerComposeCmd[1], "-f", composeFile.toString(), "down")
            }
            processBuilder.directory(composeFile.toFile().parentFile)
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            Log.log(logger::info, "started process {}", process.info())

            streamExecutor.submit(StreamGobbler(process.inputStream) {
                Log.log(logger::info, "DigmaDocker: $it")
            })
            streamExecutor.submit(StreamGobbler(process.errorStream) {
                Log.log(logger::info, "DigmaDockerError: $it")
            })

            val success = process.waitFor(5, TimeUnit.MINUTES)

            val exitValue = process.exitValue()

            if (success) {
                Log.log(logger::info, "docker compose down completed successfully [exit code {}] for {}", exitValue, process.info())
            } else {
                Log.log(logger::info, "docker compose down unsuccessful [exit code {}] for {}", exitValue, process.info())
            }

            return exitValue.toString()

        } catch (e: Exception) {
            Log.warnWithException(logger, e, "error docker compose down")
            return e.message ?: e.toString()
        }
    }

    fun start(composeFile: Path, dockerComposeCmd: List<String>): String {
        try {

            Log.log(logger::info, "starting docker compose")

            val processBuilder = ProcessBuilder()
            if (dockerComposeCmd.size == 1) {
                processBuilder.command(dockerComposeCmd[0], "-f", composeFile.toString(), "up", "-d")
            } else {
                processBuilder.command(dockerComposeCmd[0], dockerComposeCmd[1], "-f", composeFile.toString(), "up", "-d")
            }
            processBuilder.directory(composeFile.toFile().parentFile)
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            Log.log(logger::info, "started process {}", process.info())

            streamExecutor.submit(StreamGobbler(process.inputStream) {
                Log.log(logger::info, "DigmaDocker: $it")
            })
            streamExecutor.submit(StreamGobbler(process.errorStream) {
                Log.log(logger::info, "DigmaDockerError: $it")
            })

            val success = process.waitFor(5, TimeUnit.MINUTES)

            val exitValue = process.exitValue()

            if (success) {
                Log.log(logger::info, "docker compose start completed successfully [exit code {}] for {}", exitValue, process.info())
            } else {
                Log.log(logger::info, "docker compose start unsuccessful [exit code {}] for {}", exitValue, process.info())
            }

            return exitValue.toString()

        } catch (e: Exception) {
            Log.warnWithException(logger, e, "error docker compose start")
            return e.message ?: e.toString()
        }
    }


    fun stop(composeFile: Path, dockerComposeCmd: List<String>): String {
        try {

            Log.log(logger::info, "starting docker compose stop")

            val processBuilder = ProcessBuilder()
            if (dockerComposeCmd.size == 1) {
                processBuilder.command(dockerComposeCmd[0], "-f", composeFile.toString(), "stop")
            } else {
                processBuilder.command(dockerComposeCmd[0], dockerComposeCmd[1], "-f", composeFile.toString(), "stop")
            }
            processBuilder.directory(composeFile.toFile().parentFile)
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            Log.log(logger::info, "started process {}", process.info())

            streamExecutor.submit(StreamGobbler(process.inputStream) {
                Log.log(logger::info, "DigmaDocker: $it")
            })
            streamExecutor.submit(StreamGobbler(process.errorStream) {
                Log.log(logger::info, "DigmaDockerError: $it")
            })

            val success = process.waitFor(5, TimeUnit.MINUTES)

            val exitValue = process.exitValue()

            if (success) {
                Log.log(logger::info, "docker compose stop completed successfully [exit code {}] for {}", exitValue, process.info())
            } else {
                Log.log(logger::info, "docker compose stop unsuccessful [exit code {}] for {}", exitValue, process.info())
            }

            return exitValue.toString()

        } catch (e: Exception) {
            Log.warnWithException(logger, e, "error docker compose stop")
            return e.message ?: e.toString()
        }
    }


    fun remove(composeFile: Path, dockerComposeCmd: List<String>): String {
        try {

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
            processBuilder.directory(composeFile.toFile().parentFile)
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            Log.log(logger::info, "started process {}", process.info())

            streamExecutor.submit(StreamGobbler(process.inputStream) {
                Log.log(logger::info, "DigmaDocker: $it")
            })
            streamExecutor.submit(StreamGobbler(process.errorStream) {
                Log.log(logger::info, "DigmaDockerError: $it")
            })

            val success = process.waitFor(5, TimeUnit.MINUTES)

            val exitValue = process.exitValue()

            if (success) {
                Log.log(logger::info, "uninstall completed successfully [exit code {}] for {}", exitValue, process.info())
            } else {
                Log.log(logger::info, "uninstall unsuccessful [exit code {}] for {}", exitValue, process.info())
            }

            return exitValue.toString()

        } catch (e: Exception) {
            Log.warnWithException(logger, e, "error uninstalling engine")
            return e.message ?: e.toString()
        }
    }


}
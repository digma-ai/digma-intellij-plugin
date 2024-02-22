package org.digma.intellij.plugin.idea.runcfg

import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.common.Retries
import org.digma.intellij.plugin.log.Log
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.locks.ReentrantLock
import kotlin.io.path.deleteIfExists

private const val JARS_DIR_PREFIX = "digma-otel-jars"
private const val RESOURCE_LOCATION = "otelJars"
private const val OTEL_AGENT_JAR_URL =
//  "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar"
    "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.1.0/opentelemetry-javaagent.jar"
private const val DIGMA_AGENT_EXTENSION_JAR_URL =
    "https://github.com/digma-ai/otel-java-instrumentation/releases/latest/download/digma-otel-agent-extension.jar"
private const val OTEL_AGENT_JAR_NAME = "opentelemetry-javaagent.jar"
private const val DIGMA_AGENT_EXTENSION_JAR_NAME = "digma-otel-agent-extension.jar"


//Do not change to light service with annotation because this service is only necessary for java in idea
class OTELJarProvider {

    private val logger: Logger = Logger.getInstance(this::class.java)

    private val downloadDir: File = File(System.getProperty("java.io.tmpdir"), JARS_DIR_PREFIX)

    private val lock = ReentrantLock()


    init {
        //unpack and download on service initialization.
        //will happen per IDE session
        Thread {
            unpackFilesAndDownloadLatest()
        }.start()
    }


    fun getOtelAgentJarPath(): String? {
        ensureFilesExist()
        val otelJar = getOtelAgentJar()
        if (otelJar.exists()) {
            return otelJar.absolutePath
        }
        return null
    }

    private fun getOtelAgentJar(): File {
        return File(downloadDir, OTEL_AGENT_JAR_NAME)
    }

    fun getDigmaAgentExtensionJarPath(): String? {
        ensureFilesExist()
        val digmaJar = getDigmaAgentExtensionJar()
        if (digmaJar.exists()) {
            return digmaJar.absolutePath
        }
        return null
    }

    private fun getDigmaAgentExtensionJar(): File {
        return File(downloadDir, DIGMA_AGENT_EXTENSION_JAR_NAME)
    }


    private fun ensureFilesExist() {

        if (filesExist()) {
            return
        }

        Log.log(logger::info, "otel jars do not exists, unpacking..")

        unpackFilesAndDownloadLatest()

    }

    private fun filesExist(): Boolean {
        val otelJar = getOtelAgentJar()
        val digmaJar = getDigmaAgentExtensionJar()
        return otelJar.exists() && digmaJar.exists()
    }


    private fun unpackFilesAndDownloadLatest() {

        Log.log(logger::info, "unpacking otel agent jars")

        withLock {
            try {
                if (!downloadDir.exists()) {
                    if (!downloadDir.mkdirs()) {
                        Log.log(logger::warn, "could not create directory for otel jars {}", downloadDir)
                    }
                }

                if (downloadDir.exists()) {
                    copyFileFromResource(OTEL_AGENT_JAR_NAME)
                    copyFileFromResource(DIGMA_AGENT_EXTENSION_JAR_NAME)
                    Log.log(logger::info, "otel agent jars unpacked to {}", downloadDir)
                }
            } catch (e: Exception) {
                Log.warnWithException(logger, e, "could not unpack otel jars, hopefully download will succeed.")
            }
        }

        tryDownloadLatest()
    }


    private fun copyFileFromResource(fileName: String) {
        val inputStream = this::class.java.getResourceAsStream("/$RESOURCE_LOCATION/$fileName")
        if (inputStream == null) {
            Log.log(logger::warn, "could not find file in resource folder {}", fileName)
            return
        }

        val file = File(downloadDir, fileName)
        val outputStream = FileOutputStream(file)
        Log.log(logger::info, "unpacking {} to {}", fileName, file)
        com.intellij.openapi.util.io.StreamUtil.copy(inputStream, outputStream)
    }


    private fun tryDownloadLatest() {

        Log.log(logger::info, "trying to download latest otel jars")

        val runnable = Runnable {

            try {
                val otelUrl = System.getProperty("org.digma.otel.agentUrl",OTEL_AGENT_JAR_URL)
                val extensionUrl = System.getProperty("org.digma.otel.extensionUrl",DIGMA_AGENT_EXTENSION_JAR_URL)

                downloadAndCopyJar(URL(otelUrl), getOtelAgentJar())
                downloadAndCopyJar(URL(extensionUrl), getDigmaAgentExtensionJar())
            } catch (e: Exception) {
                Log.warnWithException(logger, e, "could not download latest otel jars")
            }
        }

        Thread(runnable).start()
    }


    private fun downloadAndCopyJar(url: URL, toFile: File) {

        val tempFile = kotlin.io.path.createTempFile("tempJarFile", ".jar")

        try {

            Retries.simpleRetry({

                Log.log(logger::info, "downloading {}", url)

                val connection = url.openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                connection.getInputStream().use {
                    Files.copy(it, tempFile, StandardCopyOption.REPLACE_EXISTING)
                }

                withLock {
                    Log.log(logger::info, "copying downloaded file {} to {}", tempFile, toFile)
                    try {
                        Files.move(tempFile, toFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                    } catch (e: Exception) {
                        //ATOMIC_MOVE is not always supported so try again on exception
                        Files.move(tempFile, toFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                }

            }, Throwable::class.java, 5000, 3)

        } catch (e: Exception) {
            Log.log(logger::warn, "could not download file {}, {}", url, e)
        } finally {
            tempFile.deleteIfExists()
        }
    }


    private fun withLock(function: () -> Unit) {
        try {
            lock.lock()
            function()
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }

}
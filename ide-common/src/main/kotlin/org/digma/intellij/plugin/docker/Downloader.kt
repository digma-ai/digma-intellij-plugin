package org.digma.intellij.plugin.docker

import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.common.Retries
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.errorreporting.SEVERITY_HIGH_TRY_FIX
import org.digma.intellij.plugin.errorreporting.SEVERITY_PROP_NAME
import org.digma.intellij.plugin.log.Log
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.deleteIfExists

private const val COMPOSE_FILE_URL = "https://get.digma.ai/"
private const val COMPOSE_FILE_NAME = "docker-compose.yml"
const val COMPOSE_FILE_DIR = "digma-docker"
private const val RESOURCE_LOCATION = "docker-compose"

class Downloader {

    private val logger = Logger.getInstance(this::class.java)

    private val downloadDir: File = File(System.getProperty("java.io.tmpdir"), COMPOSE_FILE_DIR)
    val composeFile: File = File(downloadDir, COMPOSE_FILE_NAME)


    private fun unpackAndDownloadLatestNow() {
        unpack()
        downloadNow()
    }


    private fun unpack() {
        Log.log(logger::info, "unpacking docker-compose.yml")

        try {
            ensureDirectoryExist()

            if (downloadDir.exists()) {
                copyFileFromResource()
                Log.log(logger::info, "docker-compose.yml unpacked to {}", downloadDir)
            }
        } catch (e: Exception) {
            ErrorReporter.getInstance().reportError("Downloader.unpack", e)
            Log.warnWithException(logger, e, "could not unpack docker-compose.yml.")
        }
    }


    private fun ensureDirectoryExist() {
        if (!downloadDir.exists()) {
            if (!downloadDir.mkdirs()) {
                Log.log(logger::warn, "could not create directory for docker-compose.yml {}", downloadDir)
                ErrorReporter.getInstance().reportError(
                    null, "Downloader.ensureDirectoryExist",
                    "ensureDirectoryExist,could not create directory for docker-compose.yml in $downloadDir",
                    mapOf(
                        SEVERITY_PROP_NAME to SEVERITY_HIGH_TRY_FIX,
                        "error hint" to "could not create directory for docker-compose.yml in $downloadDir"
                    )
                )
            }
        }
    }


    private fun copyFileFromResource() {
        val inputStream = this::class.java.getResourceAsStream("/$RESOURCE_LOCATION/$COMPOSE_FILE_NAME")
        if (inputStream == null) {
            Log.log(logger::warn, "could not find file in resource for {}", COMPOSE_FILE_NAME)
            return
        }

        val outputStream = FileOutputStream(composeFile)
        Log.log(logger::info, "unpacking {} to {}", COMPOSE_FILE_NAME, composeFile)
        com.intellij.openapi.util.io.StreamUtil.copy(inputStream, outputStream)
    }



    private fun downloadNow() {
        ensureDirectoryExist()
        Log.log(logger::info, "trying to download latest compose file")
        downloadAndCopyFile(URL(COMPOSE_FILE_URL), composeFile)
    }

    private fun downloadAndCopyFile(url: URL, toFile: File) {

        val tempFile = kotlin.io.path.createTempFile("tempComposeFile", ".yml")

        try {

            Retries.simpleRetry({

                Log.log(logger::info, "downloading {}", url)

                val connection = url.openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                connection.getInputStream().use {
                    Files.copy(it, tempFile, StandardCopyOption.REPLACE_EXISTING)
                }

                Log.log(logger::info, "copying downloaded file {} to {}", tempFile, toFile)
                try {
                    Files.move(tempFile, toFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                } catch (e: Exception) {
                    //ATOMIC_MOVE is not always supported so try again on exception
                    Files.move(tempFile, toFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }

            }, Throwable::class.java, 5000, 3)

        } catch (e: Exception) {
            ErrorReporter.getInstance().reportError("Downloader.downloadAndCopyFile", e)
            Log.log(logger::warn, "could not download file {}, {}", url, e)
        } finally {
            tempFile.deleteIfExists()
        }
    }


    fun downloadComposeFile(forceDownloadLatest: Boolean = false): Boolean {

        if (composeFile.exists() && !forceDownloadLatest) {
            Log.log(logger::warn, "compose file already exists {}", composeFile)
            return true
        }

        if (composeFile.exists() && forceDownloadLatest) {
            Log.log(logger::warn, "compose file already exists but forcing download latest {}", composeFile)
            downloadNow()
            return true
        }

        //compose file does not exist. probably first install or the file was deleted.
        // in case the file was deleted this will actually upgrade the backend to latest compose file
        unpackAndDownloadLatestNow()

        return composeFile.exists()

    }


    fun deleteFile() {
        Retries.simpleRetry(Runnable {
            val dir = composeFile.parentFile
            Files.deleteIfExists(composeFile.toPath())
            Files.deleteIfExists(dir.toPath())
        }, Throwable::class.java, 100, 5)
    }

}
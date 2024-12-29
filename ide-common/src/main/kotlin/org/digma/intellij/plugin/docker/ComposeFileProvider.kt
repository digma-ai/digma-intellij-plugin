package org.digma.intellij.plugin.docker

import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.common.Retries
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.paths.DigmaPathManager
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.deleteIfExists

private const val COMPOSE_FILE_URL = "https://get.digma.ai/"
const val COMPOSE_FILE_NAME = "docker-compose.yml"
const val COMPOSE_FILE_DIR_NAME = "digma-docker"

/**
 * ComposeFileProvider is responsible for providing the docker-compose.yml file.
 * on first request for the file, if the file does not exist, It will be downloaded and saved to the local file system.
 * after that the file it will be available for running docker operations and will not be downloaded again, unless it was deleted somehow.
 * the file is saved to a persistence folder on user's machine and should not be deleted by the user. it's like an installation file
 * of the plugin and usually users will not delete it.
 * when upgrading the local engine the docker service will call downloadLatestComposeFile to download the latest compose file. the file will be downloaded
 * and override the existing file, from now on the new file will be used for docker operations.
 * whenever the file is not found the latest will be downloaded and saved.
 * This class also supports using a custom docker compose file for development purposes only. the custom file is downloaded from a custom url provided
 * with a system property. engine upgrade will not work when using a custom docker compose file.
 */
class ComposeFileProvider {

    private val logger = Logger.getInstance(this::class.java)

    private val composeFile: File = File(COMPOSE_FILE_DIR, COMPOSE_FILE_NAME)

    //customComposeFileDownloaded is used to mark if the custom compose file was already downloaded in this IDE session.
    // it needs to be downloaded only once per IDE session. after IDE restart, a new one will be downloaded and override the old one if it exists.
    // it may be that a new session has another custom compose file url so need to override the old one.
    private var customComposeFileDownloaded = false
    private val customComposeFile: File = File(CUSTOM_COMPOSE_FILE_DIR, COMPOSE_FILE_NAME)


    companion object {
        val COMPOSE_FILE_DIR = File(DigmaPathManager.getLocalFilesDirectoryPath(), COMPOSE_FILE_DIR_NAME)
        val CUSTOM_COMPOSE_FILE_DIR = File(System.getProperty("java.io.tmpdir"), COMPOSE_FILE_DIR_NAME)

        private fun usingCustomComposeFile(): Boolean {
            return getCustomComposeFileUrl() != null
        }

        /*
          Using a custom compose file is for development purposes only and not for users.
          After using it, it is necessary to remove the local engine and remove the property.
          Then install local engine regularly.
          Upgrade should not be invoked when using a custom compose file. it will not work, it will always use the same custom url.
         */
        private fun getCustomComposeFileUrl(): String? {
            return System.getProperty("org.digma.plugin.custom.docker-compose.url")
        }
    }


    //this method should not be used to get the file itself, it is mainly for logging and debugging.
    //it should not create the file or unpack it.
    //use the method getComposeFile() to get the file for running docker operations
    fun getComposeFilePath(): String {
        if (usingCustomComposeFile()) {
            return customComposeFile.absolutePath
        }
        return composeFile.absolutePath
    }



    //this method should return a file, if the file does not exist, the docker operation will fail
    fun getComposeFile(): File {
        ensureComposeFileExists()

        if (usingCustomComposeFile()) {
            return customComposeFile
        }

        return composeFile
    }


    fun ensureComposeFileExists(): Boolean {
        try {
            if (usingCustomComposeFile()) {
                Log.log(logger::info, "using custom compose file from {}", getCustomComposeFileUrl())
                return ensureCustomComposeFileExists()
            }

            if (composeFile.exists()) {
                Log.log(logger::info, "compose file exists {}", composeFile)
                return true
            }

            Log.log(logger::info, "compose file does not exist, downloading latest compose file")
            return downloadLatestComposeFile()

        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "could not ensure compose file exists")
            ErrorReporter.getInstance().reportError("ComposeFileProvider.ensureComposeFileExists", e)
            return false
        }
    }


    private fun ensureCustomComposeFileExists(): Boolean {

        if (customComposeFileDownloaded && customComposeFile.exists()) {
            return true
        }

        return getCustomComposeFileUrl()?.let { url ->
            Log.log(logger::info, "downloading custom compose file from {}", url)
            ensureDirectoryExist(CUSTOM_COMPOSE_FILE_DIR)
            val downloadResult = downloadAndCopyFile(URI(url).toURL(), customComposeFile)
            if (downloadResult) {
                customComposeFile.deleteOnExit()
                Log.log(logger::info, "custom compose file downloaded to {}", customComposeFile)
            } else {
                Log.log(logger::warn, "could not download custom compose file from {}", url)
            }
            customComposeFileDownloaded = downloadResult
            downloadResult
        } ?: false

    }


    private fun ensureDirectoryExist(dir: File) {
        try {
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.log(logger::warn, "could not create directory for docker-compose.yml {}", dir)
                    ErrorReporter.getInstance().reportError(
                        null, "ComposeFileProvider.ensureDirectoryExist",
                        "ensureDirectoryExist,could not create directory for docker-compose.yml in $dir",
                        mapOf("error hint" to "could not create directory for docker-compose.yml in $dir")
                    )
                }
            }
        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "could not ensure directory exists {}", dir)
            ErrorReporter.getInstance().reportError("ComposeFileProvider.ensureDirectoryExist", e)
        }
    }


    fun downloadLatestComposeFile(): Boolean {

        try {
            ensureDirectoryExist(COMPOSE_FILE_DIR)
            return downloadAndCopyFile(URI(COMPOSE_FILE_URL).toURL(), composeFile)
        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "could not download latest compose file")
            ErrorReporter.getInstance().reportError("ComposeFileProvider.downloadLatestComposeFile", e)
            return false
        }
    }


    private fun downloadAndCopyFile(url: URL, toFile: File): Boolean {

        val tempFile = kotlin.io.path.createTempFile("tempComposeFile", ".yml")

        try {

            Retries.simpleRetry({

                Log.log(logger::info, "downloading {}", url)

                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"
                val responseCode: Int = connection.getResponseCode()

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw RuntimeException("could not download file from $url")
                } else {
                    connection.inputStream.use {
                        Files.copy(it, tempFile, StandardCopyOption.REPLACE_EXISTING)
                    }

                    Log.log(logger::info, "copying downloaded file {} to {}", tempFile, toFile)
                    try {
                        Files.move(tempFile, toFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                    } catch (e: Exception) {
                        //ATOMIC_MOVE is not always supported, so try again on exception
                        Files.move(tempFile, toFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }, Throwable::class.java, 5000, 3)

            return true

        } catch (e: Exception) {
            Log.log(logger::warn, "could not download file {}, {}", url, e)

            ErrorReporter.getInstance().reportError(
                "ComposeFileProvider.downloadAndCopyFile", e, mapOf(
                    "url" to url.toString(),
                    "toFile" to toFile.toString()
                )
            )
            return false

        } finally {
            tempFile.deleteIfExists()
        }
    }


    fun deleteFile() {
        Retries.simpleRetry({
            val file = if (usingCustomComposeFile()) customComposeFile else composeFile
            Files.deleteIfExists(file.toPath())
        }, Throwable::class.java, 100, 5)
    }


}
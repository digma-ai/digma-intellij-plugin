package org.digma.intellij.plugin.docker

import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.common.Retries
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.paths.DigmaPathManager
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.deleteIfExists

private const val COMPOSE_FILE_URL = "https://get.digma.ai/"
const val COMPOSE_FILE_NAME = "docker-compose.yml"
private const val RESOURCE_LOCATION = "docker-compose"
const val COMPOSE_FILE_DIR_NAME = "digma-docker"

class ComposeFileProvider {

    private val logger = Logger.getInstance(this::class.java)

    private val composeFile: File = File(COMPOSE_FILE_DIR, COMPOSE_FILE_NAME)

    //used top mark if the custom compose file was already downloaded in this IDE session.
    // needs to be downloaded only once per IDE session. after IDE restart, a new one will be downloaded and override the old one if it exists.
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
          Using a custom compose file is only for development purposes only and not for users.
          After using it, it is necessary to remove the local engine and remove the property.
          Then install local engine regularly.
          Upgrade should not be invoked when using a custom compose file. it will not work, it will always use the same custom url.
         */
        private fun getCustomComposeFileUrl(): String? {
            return System.getProperty("org.digma.plugin.custom.docker-compose.url")
        }
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
                Log.log(logger::info, "using custom compose file {}", getCustomComposeFileUrl())
                return ensureCustomComposeFileExists()
            }

            if (composeFile.exists()) {
                Log.log(logger::info, "compose file exists {}", composeFile)
                return true
            }

            Log.log(logger::info, "compose file does not exist, unpacking bundled file")
            unpack()
            return true

        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "could not ensure compose file exists")
            ErrorReporter.getInstance().reportError("ComposeFileProvider.ensureComposeFileExists", e)
            return false
        }
    }


    fun downloadLatestComposeFile(): Boolean {

        try {
            //try to delete the current file, don't fail if delete fails
            deleteFile()
        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "could not delete compose file")
            ErrorReporter.getInstance().reportError("ComposeFileProvider.downloadLatestComposeFile", e)
        }

        try {
            ensureDirectoryExist()
            downloadAndCopyFile(URI(COMPOSE_FILE_URL).toURL(), composeFile)
            return composeFile.exists()
        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "could not download latest compose file")
            ErrorReporter.getInstance().reportError("ComposeFileProvider.downloadLatestComposeFile", e)
            return false
        }
    }


    private fun ensureCustomComposeFileExists(): Boolean {

        if (customComposeFileDownloaded && customComposeFile.exists()) {
            return true
        }

        return getCustomComposeFileUrl()?.let { url ->
            CUSTOM_COMPOSE_FILE_DIR.mkdirs()
            downloadAndCopyFile(URI(url).toURL(), customComposeFile)
            customComposeFileDownloaded = customComposeFile.exists()
            customComposeFile.exists()
        } ?: false

    }



    private fun unpack() {
        Log.log(logger::info, "unpacking docker-compose.yml")

        try {
            ensureDirectoryExist()

            if (COMPOSE_FILE_DIR.exists()) {
                copyComposeFileFromResource()
                Log.log(logger::info, "docker-compose.yml unpacked to {}", COMPOSE_FILE_DIR)
            }
        } catch (e: Exception) {
            ErrorReporter.getInstance().reportError("ComposeFileProvider.unpack", e)
            Log.warnWithException(logger, e, "could not unpack docker-compose.yml")
        }
    }


    private fun ensureDirectoryExist() {
        if (!COMPOSE_FILE_DIR.exists()) {
            if (!COMPOSE_FILE_DIR.mkdirs()) {
                Log.log(logger::warn, "could not create directory for docker-compose.yml {}", COMPOSE_FILE_DIR)
                ErrorReporter.getInstance().reportError(
                    null, "ComposeFileProvider.ensureDirectoryExist",
                    "ensureDirectoryExist,could not create directory for docker-compose.yml in $COMPOSE_FILE_DIR",
                    mapOf("error hint" to "could not create directory for docker-compose.yml in $COMPOSE_FILE_DIR")
                )
            }
        }
    }


    private fun copyComposeFileFromResource() {
        val inputStream = this::class.java.getResourceAsStream("/$RESOURCE_LOCATION/$COMPOSE_FILE_NAME")
        if (inputStream == null) {
            Log.log(logger::warn, "could not find file in resource for {}", COMPOSE_FILE_NAME)
            ErrorReporter.getInstance().reportError(
                null, "ComposeFileProvider.copyFileFromResource",
                "could not extract docker-compose.yml from resource", mapOf()
            )
            return
        }

        FileOutputStream(composeFile).use {
            Log.log(logger::info, "unpacking {} to {}", COMPOSE_FILE_NAME, composeFile)
            com.intellij.openapi.util.io.StreamUtil.copy(inputStream, it)
        }

    }


    private fun downloadAndCopyFile(url: URL, toFile: File) {

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

        } catch (e: Exception) {
            Log.log(logger::warn, "could not download file {}, {}", url, e)

            ErrorReporter.getInstance().reportError(
                "ComposeFileProvider.downloadAndCopyFile", e, mapOf(
                    "url" to url.toString(),
                    "toFile" to toFile.toString()
                )
            )

        } finally {
            tempFile.deleteIfExists()
        }
    }


    fun deleteFile() {
        Retries.simpleRetry({
            val file = if(usingCustomComposeFile()) customComposeFile else composeFile
            Files.deleteIfExists(file.toPath())
        }, Throwable::class.java, 100, 5)
    }


}
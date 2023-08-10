package org.digma.intellij.plugin.docker

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.SystemProperties
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.download.FileDownloader
import org.digma.intellij.plugin.log.Log
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists

const val COMPOSE_FILE_URL = "https://get.digma.ai/"
const val COMPOSE_FILE_NAME = "docker-compose.yml"

class Downloader {

    private val logger = Logger.getInstance(this::class.java)

    var composeFile: Path? = null


    fun downloadComposeFile(forceDownloadIfExists: Boolean = false): Boolean {

        createComposeFilePath()

        if (composeFile == null) {
            Log.log(logger::warn, "Could not create compose file path")
            return false
        }

        if (composeFile!!.exists() && !forceDownloadIfExists) {
            //val deleted = composeFile!!.toFile().delete()
            //Log.log(logger::warn, "Deleted old compose file {}", deleted)
            Log.log(logger::warn, "compose file already exists {}", composeFile)
            return true
        }

        val downloadDir = composeFile!!.toFile().parentFile

        val fileDesc = DownloadableFileService.getInstance().createFileDescription(COMPOSE_FILE_URL, COMPOSE_FILE_NAME)

        val downloader = DownloadableFileService.getInstance().createDownloader(listOf(fileDesc), "downloading digma engine config")

        composeFile = downloadWithProgress(downloader, downloadDir)

        return composeFile != null

    }


    //todo: backwards compatibility support for users that installed local engine before
    // we changed to save the is-installed in persistence
    // can be removed in few versions
    fun findOldComposeFile(): Boolean {
        val homeDir = SystemProperties.getUserHome()
        val downloadDir = File(homeDir, "digma")
        val composeFile = File(downloadDir, COMPOSE_FILE_NAME)
        return composeFile.exists()
    }


    //todo: backwards compatibility support for users that installed local engine before
    // we changed to save the is-installed in persistence
    // can be removed in few versions
    fun deleteOldFileIfExists() {
        //delete file in user home if exists
        try {
            val homeDir = SystemProperties.getUserHome()
            val downloadDir = File(homeDir, "digma")
            val composeFile = File(downloadDir, COMPOSE_FILE_NAME)
            val dir = composeFile.parentFile
            if (composeFile.exists()) {
                composeFile.delete()
            }
            if (dir.exists()) {
                dir.delete()
            }
        } catch (e: Exception) {
            //ignore
        }

    }


    private fun createComposeFilePath() {
        val tmpDir = System.getProperty("java.io.tmpdir", SystemProperties.getUserHome())
        val downloadDir = File(tmpDir, "digma")
        composeFile = File(downloadDir, COMPOSE_FILE_NAME).toPath()
    }


    private fun downloadWithProgress(downloader: FileDownloader, downloadDir: File): Path? {

        repeat((1..3).count()) {
            val file = try {

                val files = downloader.downloadFilesWithProgress(downloadDir.absolutePath, null, null)

                files?.let {
                    if (it.isNotEmpty()) {
                        it[0].toNioPath()
                    } else {
                        null
                    }
                }

            } catch (e: Exception) {
                Log.debugWithException(logger, e, "Could not download docker compose file")
                null
            }

            if (file != null) {
                return file
            }
        }


        return null
    }

    fun deleteFile() {
        if (composeFile == null) {
            createComposeFilePath()
        }

        if (composeFile != null && composeFile!!.toFile().exists()) {
            composeFile!!.toFile().delete()
        }
    }

}
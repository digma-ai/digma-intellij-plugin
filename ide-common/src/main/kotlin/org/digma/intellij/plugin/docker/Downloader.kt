package org.digma.intellij.plugin.docker

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.SystemProperties
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.download.FileDownloader
import org.digma.intellij.plugin.log.Log
import java.io.File
import java.nio.file.Path

const val COMPOSE_FILE_URL = "https://get.digma.ai/"
const val COMPOSE_FILE_NAME = "docker-compose.yml"

class Downloader {

    private val logger = Logger.getInstance(this::class.java)

    var composeFile: Path? = null


    fun downloadComposeFile(): Boolean {

        createComposeFilePath()

        if (composeFile == null) {
            Log.log(logger::warn, "Could not create compose file path")
            return false
        }

        if (composeFile!!.toFile().exists()) {
            val deleted = composeFile!!.toFile().delete()
            Log.log(logger::warn, "Deleted old compose file {}", deleted)
        }

        val downloadDir = composeFile!!.toFile().parentFile

        val fileDesc = DownloadableFileService.getInstance().createFileDescription(COMPOSE_FILE_URL, COMPOSE_FILE_NAME)

        val downloader = DownloadableFileService.getInstance().createDownloader(listOf(fileDesc), "digma docker compose")

        composeFile = downloadWithProgress(downloader, downloadDir)

        return composeFile != null

    }


    fun findComposeFile(): Boolean {
        createComposeFilePath()
        return composeFile != null && composeFile!!.toFile().exists()
    }


    private fun createComposeFilePath() {
        val homeDir = SystemProperties.getUserHome()
        val downloadDir = File(homeDir, "digma")
        composeFile = File(downloadDir, COMPOSE_FILE_NAME).toPath()
    }


    private fun downloadWithProgress(downloader: FileDownloader, downloadDir: File): Path? {
        try {
            val files = downloader.downloadFilesWithProgress(downloadDir.absolutePath, null, null)

            return files?.let {
                return if (it.isNotEmpty()) {
                    it[0].toNioPath()
                } else {
                    null
                }
            }

        } catch (e: Exception) {
            Log.debugWithException(logger, e, "Could not download docker compose file")
        }

        return null
    }

}
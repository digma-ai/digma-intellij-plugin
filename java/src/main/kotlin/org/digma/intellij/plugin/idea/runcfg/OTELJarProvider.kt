package org.digma.intellij.plugin.idea.runcfg

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.download.FileDownloader
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.log.Log
import java.io.File
import java.util.concurrent.locks.ReentrantLock

private const val TEMP_JARS_DIR_PREFIX = "temp-digma-otel-jars"

class OTELJarProvider {

    private val logger: Logger = Logger.getInstance(OTELJarProvider::class.java)

    private var downloadDir: File? = null

    private val lock = ReentrantLock()

    companion object{
        @JvmStatic
        fun getInstance():OTELJarProvider{
            return ApplicationManager.getApplication().getService(OTELJarProvider::class.java)
        }
    }


    fun getOtelAgentJarPath(project: Project):String?{
        if (!ensureDownloaded(project)){
            return null
        }
        val otelJar = getOtelAgentJar(project)
        otelJar?.let {
            if (it.exists()){
                return it.absolutePath
            }
        }
        return null
    }

    private fun getOtelAgentJar(project: Project):File?{
        downloadDir?.let {
            return File(it, OTEL_AGENT_JAR_NAME)
        }
        return null
    }


    fun getDigmaAgentExtensionJarPath(project: Project):String?{
        if (!ensureDownloaded(project)){
            return null
        }
        val digmaJar = getDigmaAgentExtensionJar(project)
        digmaJar?.let {
            if (it.exists()){
                return it.absolutePath
            }
        }
        return null
    }

    private fun getDigmaAgentExtensionJar(project: Project):File?{
        downloadDir?.let {
            return File(it, DIGMA_AGENT_EXTENSION_JAR_NAME)
        }
        return null
    }


    fun ensureDownloaded(project: Project,startup: Boolean = false): Boolean {
        lock.lock()
        try {
            if (filesExist(project)){
                return true
            }
            if (downloadDir == null || !filesExist(project)) {
                downloadDir = FileUtil.createTempDirectory(TEMP_JARS_DIR_PREFIX, null, true)
                deleteOldDirsThatMayStillBeThere(downloadDir)
                Log.log(logger::debug,"downloading otel agent jar to {}", downloadDir)
                downloadJars(project,downloadDir!!,startup)
                //on startup the download is in background so just return true
                if (startup){
                    return true
                }

                if (!filesExist(project)){
                    downloadDir!!.delete()
                    downloadDir = null
                    return false
                }
            }
        } finally {
            lock.unlock()
        }
        return true
    }

    private fun deleteOldDirsThatMayStillBeThere(nextDownloadDir: File?) {
        nextDownloadDir?.let { dir ->
            val parentDir = dir.parentFile
            parentDir?.let {parentDir ->
                parentDir.listFiles { file ->
                    !file.name.equals(nextDownloadDir.name) && file.isDirectory && file.name.contains(TEMP_JARS_DIR_PREFIX)
                }.forEach { dirToDelete ->
                    Log.log(logger::debug,"deleting old temp dir {}", dirToDelete)
                    dirToDelete.delete()
                }
            }
        }
    }

    private fun filesExist(project: Project): Boolean {
        val otelJar = getOtelAgentJar(project)
        val digmaJar = getDigmaAgentExtensionJar(project)
        return  (otelJar != null && otelJar.exists()) && (digmaJar != null && digmaJar.exists())
    }


    private fun downloadJars(project: Project, downloadDir: File,startup: Boolean) {
        val otelAgentFileDescription = DownloadableFileService.getInstance().createFileDescription(OTEL_AGENT_JAR_URL,
            OTEL_AGENT_JAR_NAME)
        val digmaExtensionFileDescription = DownloadableFileService.getInstance().createFileDescription(
            DIGMA_AGENT_EXTENSION_JAR_URL, DIGMA_AGENT_EXTENSION_JAR_NAME)
        val downloader = DownloadableFileService.getInstance().createDownloader(listOf(otelAgentFileDescription,digmaExtensionFileDescription),"otel agent jars")
        download(downloader,project,downloadDir,startup)
    }



    private fun download(downloader: FileDownloader, project: Project, downloadDir: File,startup: Boolean){
        if (startup){
            downloadInBackground(downloader,project, downloadDir)
        }else{
            downloadWithProgress(downloader,project, downloadDir)
        }
    }

    private fun downloadInBackground(downloader: FileDownloader, project: Project, downloadDir: File) {

        Backgroundable.ensureBackground(project, "download otel agent jars") {
            try {
                downloader.download(downloadDir)
                Log.log(logger::debug,"otel agent jars downloaded to {}",downloadDir)
            } catch (e: Exception) {
                Log.debugWithException(logger, e, "Could not download otel agent jars")
            }
        }
    }


    private fun downloadWithProgress(downloader: FileDownloader, project: Project, downloadDir: File) {
        try {
            val files = downloader.downloadFilesWithProgress(downloadDir.absolutePath, project, null)
            if (files != null && files.size == 2) {
                Log.log(logger::debug, "otel agent jars downloaded to {}", downloadDir)
            } else {
                Log.log(logger::debug, "Could not download otel agent jars")
            }
        } catch (e: Exception) {
            Log.debugWithException(logger, e, "Could not download otel agent jars")
        }
    }


}
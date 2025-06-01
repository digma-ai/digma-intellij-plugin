package org.digma.intellij.plugin.discovery

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.ensureActive
import org.digma.intellij.plugin.discovery.model.FileDiscoveryInfo
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.psi.LanguageServiceProvider
import kotlin.coroutines.coroutineContext

@Service(Service.Level.PROJECT)
class FileDiscoveryInfoBuilder(private val project: Project) {

    private val logger = thisLogger()

    companion object {
        @JvmStatic
        fun getInstance(project: Project): FileDiscoveryInfoBuilder {
            return project.service<FileDiscoveryInfoBuilder>()
        }
    }


    suspend fun buildFileInfo(file: VirtualFile): FileDiscoveryInfo {
        Log.trace(logger, project, "building file discovery info for {}", file.url)
        val languageService = LanguageServiceProvider.getInstance(project).getLanguageService(file)
        if (languageService == null) {
            Log.trace(logger, project, "LanguageService for file {} is null", file.url)
            return FileDiscoveryInfo(file)
        }

        coroutineContext.ensureActive()

        Log.trace(logger, project, "LanguageService for file {} is {}", file.url, languageService.javaClass.name)

        val discoveryProvider = languageService.getDiscoveryProvider()
        Log.trace(logger, project, "discoveryProvider for file {} is {}", file.url, discoveryProvider.javaClass.name)
        val fileDiscoveryInfo = discoveryProvider.discover(project, file)
        Log.trace(logger, project, "fileDiscoveryInfo for file {} is {}", file.url, fileDiscoveryInfo)
        return fileDiscoveryInfo
    }

}
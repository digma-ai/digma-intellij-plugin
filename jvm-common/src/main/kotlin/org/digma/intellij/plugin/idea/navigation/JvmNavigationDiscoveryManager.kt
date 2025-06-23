package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.indexing.ID
import kotlinx.coroutines.CoroutineScope
import org.digma.intellij.plugin.discovery.AbstractNavigationDiscoveryManager
import org.digma.intellij.plugin.discovery.model.FileDiscoveryInfo
import org.digma.intellij.plugin.idea.index.JVM_CANDIDATE_FILES_INDEX_ID
import org.digma.intellij.plugin.idea.index.getJvmCandidateFilesForDiscoveryIndexInstance


@Suppress("LightServiceMigrationCode")
class JvmNavigationDiscoveryManager(project: Project, cs: CoroutineScope) : AbstractNavigationDiscoveryManager(project, cs,) {


    companion object {
        @JvmStatic
        fun getInstance(project: Project): JvmNavigationDiscoveryManager {
            return project.service<JvmNavigationDiscoveryManager>()
        }
    }


    override fun addIndexListener() {
        getJvmCandidateFilesForDiscoveryIndexInstance()?.addListener(this, this)
    }

    override fun getIndexId(): ID<String, Void> {
        return JVM_CANDIDATE_FILES_INDEX_ID
    }

    override suspend fun processFileInfo(fileInfo: FileDiscoveryInfo) {
        JvmSpanNavigationProvider.getInstance(project).processFileInfo(fileInfo)
        JvmEndpointNavigationProvider.getInstance(project).processFileInfo(fileInfo)
    }

    override suspend fun getDiscoveryStatus(): String {
        return buildString {
            appendLine(JvmSpanNavigationProvider.getInstance(project).status())
            appendLine(JvmEndpointNavigationProvider.getInstance(project).status())
        }
    }

    override suspend fun maintenance() {
        JvmSpanNavigationProvider.getInstance(project).maintenance()
        JvmEndpointNavigationProvider.getInstance(project).maintenance()
    }
}
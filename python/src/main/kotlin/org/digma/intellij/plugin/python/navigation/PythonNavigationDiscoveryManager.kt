package org.digma.intellij.plugin.python.navigation

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.indexing.ID
import kotlinx.coroutines.CoroutineScope
import org.digma.intellij.plugin.discovery.AbstractNavigationDiscoveryManager
import org.digma.intellij.plugin.discovery.model.FileDiscoveryInfo
import org.digma.intellij.plugin.python.index.PYTHON_CANDIDATE_FILES_INDEX_ID
import org.digma.intellij.plugin.python.index.getPythonCandidateFilesForDiscoveryIndexInstance


@Suppress("LightServiceMigrationCode")
class PythonNavigationDiscoveryManager(project: Project, cs: CoroutineScope) : AbstractNavigationDiscoveryManager(project, cs,) {


    companion object {
        @JvmStatic
        fun getInstance(project: Project): PythonNavigationDiscoveryManager {
            return project.service<PythonNavigationDiscoveryManager>()
        }
    }


    override fun addIndexListener() {
        getPythonCandidateFilesForDiscoveryIndexInstance()?.addListener(this, this)
    }

    override fun getIndexId(): ID<String, Void> {
        return PYTHON_CANDIDATE_FILES_INDEX_ID
    }

    override suspend fun processFileInfo(fileInfo: FileDiscoveryInfo) {
        PythonSpanNavigationProvider.getInstance(project).processFileInfo(fileInfo)
    }

    override suspend fun getDiscoveryStatus(): String {
        return PythonSpanNavigationProvider.getInstance(project).status()
    }

    override suspend fun maintenance() {
        PythonSpanNavigationProvider.getInstance(project).maintenance()
    }
}
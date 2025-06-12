package org.digma.intellij.plugin.python.index

import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexExtension.EXTENSION_POINT_NAME


//can't put a getInstance in a companion object on extension because jetbrains warn about it.
//must support nullable, but it should always succeed.
fun getPythonCandidateFilesForDiscoveryIndexInstance(): PythonCandidateFilesForDiscoveryDetectionIndex? {
    return EXTENSION_POINT_NAME.findExtension<PythonCandidateFilesForDiscoveryDetectionIndex>(PythonCandidateFilesForDiscoveryDetectionIndex::class.java)
}

suspend fun hasIndex(project: Project, file: VirtualFile): Boolean {
    return smartReadAction(project) {
        FileBasedIndex.getInstance().getFileData(PYTHON_CANDIDATE_FILES_INDEX_ID, file, project).isNotEmpty()
    }
}
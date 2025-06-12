package org.digma.intellij.plugin.idea.index

import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexExtension

suspend fun hasIndex(project: Project, file: VirtualFile): Boolean {
    return smartReadAction(project) {
        FileBasedIndex.getInstance().getFileData(JVM_CANDIDATE_FILES_INDEX_ID, file, project).isNotEmpty()
    }
}

//can't put a getInstance in a companion object on extension because jetbrains warn about it.
//must support nullable, but it should always succeed.
fun getJvmCandidateFilesForDiscoveryIndexInstance(): JvmCandidateFilesForDiscoveryDetectionIndex? {
    return FileBasedIndexExtension.EXTENSION_POINT_NAME.findExtension<JvmCandidateFilesForDiscoveryDetectionIndex>(JvmCandidateFilesForDiscoveryDetectionIndex::class.java)
}
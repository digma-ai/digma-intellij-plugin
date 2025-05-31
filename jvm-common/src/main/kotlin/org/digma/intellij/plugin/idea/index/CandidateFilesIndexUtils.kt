package org.digma.intellij.plugin.idea.index

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.FileBasedIndex

suspend fun hasIndex(project: Project, file: VirtualFile): Boolean {
    return readAction {
        FileBasedIndex.getInstance().getFileData(CANDIDATE_FILES_INDEX_ID, file, project).isNotEmpty()
    }
}
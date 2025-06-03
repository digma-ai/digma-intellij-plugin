package org.digma.intellij.plugin.idea.index

import com.intellij.openapi.vfs.VirtualFile
import java.util.EventListener

interface CandidateFilesDetectionIndexListener : EventListener {
    fun fileUpdated(file: VirtualFile, keys: List<String>)
}
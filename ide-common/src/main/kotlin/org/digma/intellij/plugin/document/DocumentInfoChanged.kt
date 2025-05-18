package org.digma.intellij.plugin.document

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import org.digma.intellij.plugin.model.discovery.DocumentInfo


interface DocumentInfoChanged {


    companion object {
        @JvmField
        @Topic.ProjectLevel
        val DOCUMENT_INFO_CHANGED_TOPIC: Topic<DocumentInfoChanged> =
            Topic.create<DocumentInfoChanged>("DOCUMENT_INFO_CHANGE_TOPIC", DocumentInfoChanged::class.java)
    }


    fun documentInfoChanged(file: VirtualFile, documentInfo: DocumentInfo)
}

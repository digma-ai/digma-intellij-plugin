package org.digma.intellij.plugin.document

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Storage for DocumentInfo mapped by their VirtualFile.
 * VirtualFile objects don't change for the lifetime of the IDE.
 * When a file is renamed or moved, the VirtualFile is the same object, so renaming a file currently
 * opened in the editor will have the same VirtualFile, and thus the key here is the same key.
 */
@Service(Service.Level.PROJECT)
class DocumentInfoStorage(private val project: Project) {

    private val logger = Logger.getInstance(this::class.java)

    private val documentInfos: ConcurrentMap<VirtualFile, DocumentInfoContainer> = ConcurrentHashMap()

    companion object {
        @JvmStatic
        fun getInstance(project: Project): DocumentInfoStorage {
            return project.service<DocumentInfoStorage>()
        }
    }

    fun allDocumentInfos(): List<DocumentInfo> {
        return documentInfos.values.map { it.getDocumentInfo() }.toList()
    }


    fun allFiles(): Set<VirtualFile> {
        return documentInfos.keys.toSet()
    }

    //initially I wanted to keep putDocumentInfo internal because there is no reason to put a documentInfo from other classes
    // other than EditorDocumentService. but this is a solution for the Rider issue. see comment in call site.
    fun updateDocumentInfoForRider(file: VirtualFile, documentInfo: DocumentInfo) {
        Log.trace(logger, project, "updateDocumentInfoForRider: {} , DocumentInfo:[{}]", file, documentInfo)
        putDocumentInfo(file, documentInfo)
    }

    internal fun putDocumentInfo(file: VirtualFile, documentInfo: DocumentInfo) {

        if (logger.isTraceEnabled) {
            Log.trace(logger, project, "putDocumentInfo: {} , DocumentInfo:[{}]", file, documentInfo)
            if (containsDocumentInfo(file)) {
                Log.trace(logger, project, "putDocumentInfo: file already exists , assuming update. {}", file)
            }
        }

        val existing = documentInfos[file]
        val container = DocumentInfoContainer(documentInfo)
        documentInfos[file] = container

        if (logger.isTraceEnabled) {
            Log.log(
                logger::trace,
                "currently have {} documents in storage: {}",
                documentInfos.size,
                documentInfos.keys.joinToString(", ") { it.path })
        }

        if (existing?.getDocumentInfo() != documentInfo) {
            fireDocumentInfoChanged(file, documentInfo)
        }
    }


    private fun fireDocumentInfoChanged(
        file: VirtualFile,
        documentInfo: DocumentInfo
    ) {
        Log.trace(logger, project, "fireDocumentInfoChanged: for {}", file)
        project.messageBus.syncPublisher(DocumentInfoChanged.DOCUMENT_INFO_CHANGED_TOPIC).documentInfoChanged(file, documentInfo)
    }


    internal fun removeDocumentInfo(file: VirtualFile) {
        documentInfos.remove(file)
        if (logger.isTraceEnabled) {
            Log.trace(logger, project, "removeDocumentInfo: file:{}", file)
            Log.log(
                logger::trace,
                "currently have {} documents in storage: {}",
                documentInfos.size,
                documentInfos.keys.joinToString(", ") { it.path })
        }

        fireDocumentInfoRemoved(file)
    }

    private fun fireDocumentInfoRemoved(file: VirtualFile) {
        Log.trace(logger, project, "fireDocumentInfoRemoved: for {}", file)
        project.messageBus.syncPublisher(DocumentInfoChanged.DOCUMENT_INFO_CHANGED_TOPIC).documentInfoRemoved(file)
    }

    fun containsDocumentInfo(file: VirtualFile): Boolean {
        return documentInfos[file] != null
    }

    fun getDocumentInfo(file: VirtualFile): DocumentInfo? {
        return documentInfos[file]?.getDocumentInfo()
    }


    private inner class DocumentInfoContainer(private val documentInfo: DocumentInfo) {
        fun getDocumentInfo(): DocumentInfo {
            return documentInfo
        }
    }
}
package org.digma.intellij.plugin.document

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log

class EditorEventsHandler(private val project: Project) : FileEditorManagerListener {

    private val logger = Logger.getInstance(this::class.java)


    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        try {
            if (logger.isTraceEnabled) {
                Log.log(logger::trace, "fileOpened: {}", file)
            }

            if (!isProjectValid(project)) {
                return
            }

            EditorDocumentService.getInstance(project).fileOpened(file)

        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "Exception in fileOpened {}", e)
            ErrorReporter.getInstance().reportError(project, "EditorEventsHandler.fileOpened", e)
        }
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        try {

            if (logger.isTraceEnabled) {
                Log.log(logger::trace, "fileClosed: {}", file)
            }

            if (!isProjectValid(project)) {
                return
            }

            EditorDocumentService.getInstance(project).fileClosed(file)

        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "Exception in fileClosed {}", e)
            ErrorReporter.getInstance().reportError(project, "EditorEventsHandler.fileClosed", e)
        }
    }


    //selection changed is implemented in case building document info during fileOpened failed, maybe the failure
    // was temporary and next time it will succeed.
    //todo: this code will run on EDT for every selection changed , it should be very fast to avoid slowing down the UI.
    // consider removing it, worst case if building document info during fileOpened failed then code lens will not work for this document.
//    override fun selectionChanged(event: FileEditorManagerEvent) {
//        try {
//
//            if (logger.isTraceEnabled) {
//                Log.log(logger::trace, "selectionChanged: new {}", event.newFile)
//            }
//
//            if (!isProjectValid(project)) {
//                return
//            }
//
//            val file = event.newFile ?: return
//
//            //this should be a fast check, if the file is not relevant, don't do anything
//            if (!isRelevantVirtualFile(project, file)) {
//                return
//            }
//
//            if (DocumentInfoStorage.getInstance(project).containsDocumentInfo(file)) {
//                return
//            }
//
//            val editor = getSelectedTextEditorForFile(file, event.manager) ?: return
//
//            EditorDocumentService.getInstance(project).fileOpened(file, editor)
//
//        } catch (e: Throwable) {
//            Log.warnWithException(logger, e, "Exception in selectionChanged {}", e)
//            ErrorReporter.getInstance().reportError(project, "EditorEventsHandler.selectionChanged", e)
//        }
//    }

}
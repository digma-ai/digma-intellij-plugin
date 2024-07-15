package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.psi.PsiManager
import org.digma.intellij.plugin.bulklistener.AbstractBulkFileChangeListener
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.runInReadAccessWithResult
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.psi.isJvmSupportedFile
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.psi.PsiUtils

class BulkFileChangeListenerForJvmNavigationDiscovery : AbstractBulkFileChangeListener() {


    override fun processEvents(project: Project, events: List<VFileEvent>) {
        //processEvents runs on a background thread for all open projects

        if (!isProjectValid(project)) {
            return
        }


        events.forEach { fileEvent: VFileEvent ->

            Log.log(logger::trace, "got VFileEvent {}", fileEvent)

            try {

                if (isRelevantFile(project, fileEvent.file)) {

                    when (fileEvent) {

                        is VFileDeleteEvent -> {
                            deleteFromNavigation(project, fileEvent.file)
                        }

                        is VFilePropertyChangeEvent -> {
                            deleteFromNavigationByOldPath(project, fileEvent.oldPath)
                            updateNavigation(project, fileEvent.file)
                        }

                        is VFileMoveEvent -> {
                            deleteFromNavigationByOldPath(project, fileEvent.oldPath)
                            updateNavigation(project, fileEvent.file)
                        }

                        is VFileCopyEvent -> {
                            fileEvent.findCreatedFile()?.let { newFile ->
                                updateNavigation(project, newFile)
                            }
                        }

                        else -> {
                            fileEvent.file?.let {
                                updateNavigation(project, it)
                            }
                        }
                    }
                }

            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "Exception in processEvents for {}", fileEvent)
                ErrorReporter.getInstance().reportError(
                    project, "BulkFileChangeListenerForJvmNavigationDiscovery.processEvents", e,
                    mapOf(
                        "fileEvent" to fileEvent.toString()
                    )
                )
            }
        }
    }


    private fun updateNavigation(project: Project, file: VirtualFile) {
        if (isValidRelevantFile(project, file)) {
            val psiFile = runInReadAccessWithResult {
                PsiManager.getInstance(project).findFile(file)
            }
            psiFile?.let {
                if (PsiUtils.isValidPsiFile(it) && isJvmSupportedFile(project, it)) {
                    Log.log(logger::trace, "calling fileChanged for {}", file)
                    JvmSpanNavigationProvider.getInstance(project).fileChanged(file)
                    JvmEndpointNavigationProvider.getInstance(project).fileChanged(file)
                }
            }
        }
    }


    private fun deleteFromNavigation(project: Project, file: VirtualFile) {
        //deleted file may be any file. a java/kotlin file will not be in content anymore and will be invalid but
        // will still have a path, so we can remove it from discovery.
        //try to remove from discovery no matter which file is it, worst case nothing will happen if it's a file that
        // was never scanned by navigation discovery.
        Log.log(logger::trace, "calling fileDeleted for {}", file)
        JvmSpanNavigationProvider.getInstance(project).fileDeleted(file)
        JvmEndpointNavigationProvider.getInstance(project).fileDeleted(file)
    }

    private fun deleteFromNavigationByOldPath(project: Project, oldPath: String) {
        Log.log(logger::trace, "calling pathDeleted for {}", oldPath)
        JvmSpanNavigationProvider.getInstance(project).pathDeleted(oldPath)
        JvmEndpointNavigationProvider.getInstance(project).pathDeleted(oldPath)
    }

}
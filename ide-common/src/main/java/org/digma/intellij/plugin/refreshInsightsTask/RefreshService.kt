package org.digma.intellij.plugin.refreshInsightsTask

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.withUiContext
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.RunnableCallable
import com.intellij.util.concurrency.NonUrgentExecutor
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.document.DocumentInfoContainer
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.service.ErrorsViewService
import org.digma.intellij.plugin.ui.service.InsightsViewService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

class RefreshService(private val project: Project) {
    private val logger: Logger = Logger.getInstance(RefreshService::class.java)

    private val errorsViewService: ErrorsViewService = project.getService(ErrorsViewService::class.java)
    private val insightsViewService: InsightsViewService = project.getService(InsightsViewService::class.java)
    private val refreshInsightsTaskScheduledLock: ReentrantLock = ReentrantLock()
    private val isGeneralRefreshButtonEnabled = AtomicBoolean(true)

    companion object {
        fun getInstance(project: Project): RefreshService {
            return project.getService(RefreshService::class.java)
        }
    }

    suspend fun refreshAllForCurrentFile(file: VirtualFile) {
        Log.log(logger::debug, "Automatic refreshAllForCurrentFile started for file = {}", file.name)
        val scope = insightsViewService.model.scope
        val selectedTextEditor = withUiContext {
            // this code is on the UI thread
            FileEditorManager.getInstance(project).selectedTextEditor
        }
        if (scope is MethodScope) {
            val documentInfoContainer = DocumentInfoService.getInstance(project).getDocumentInfoByMethodInfo(scope.getMethodInfo())

            Log.log(logger::debug, "updateInsightsCacheForActiveDocument starts for file = {}", file.name)
            updateInsightsCacheForActiveDocument(selectedTextEditor, documentInfoContainer, scope)
            Log.log(logger::debug, "updateInsightsCacheForActiveDocument finished for file = {}", file.name)
        }
    }

    fun refreshAllInBackground() {
        if (isGeneralRefreshButtonEnabled.getAndSet(false)) {
            val scope = insightsViewService.model.scope
            val selectedTextEditor = FileEditorManager.getInstance(project).selectedTextEditor
            if (scope is MethodScope) {
                val documentInfoContainer = DocumentInfoService.getInstance(project).getDocumentInfoByMethodInfo(scope.getMethodInfo())

                //updateInsightsCache must run in the background, the use of refreshInsightsTaskScheduledLock makes sure no two threads
                //update InsightsCache at the same time.
                val task = Runnable {
                    refreshInsightsTaskScheduledLock.lock()
                    Log.log(logger::debug, "Lock acquired for refreshAll to {}. ", documentInfoContainer?.documentInfo?.fileUri)
                    try {
                        notifyRefreshInsightsTaskStarted(documentInfoContainer?.documentInfo?.fileUri)
                        updateInsightsCacheForActiveDocument(selectedTextEditor, documentInfoContainer, scope)
                    } finally {
                        refreshInsightsTaskScheduledLock.unlock()
                        Log.log(logger::debug, "Lock released for refreshAll to {}. ", documentInfoContainer?.documentInfo?.fileUri)
                        notifyRefreshInsightsTaskFinished(documentInfoContainer?.documentInfo?.fileUri)
                        isGeneralRefreshButtonEnabled.set(true)
                    }
                }
                Backgroundable.ensureBackground(project, "Refreshing insights", task)
            }
        }
    }

    private fun updateInsightsCacheForActiveDocument(selectedTextEditor: Editor?, documentInfoContainer: DocumentInfoContainer?, scope: MethodScope) {
        val selectedDocument = selectedTextEditor?.document
        if (selectedDocument != null) {
            documentInfoContainer?.updateCache()
        }
        ReadAction.nonBlocking(RunnableCallable{
            insightsViewService.updateInsightsModel(scope.getMethodInfo())
            errorsViewService.updateErrorsModel(scope.getMethodInfo())
        }).inSmartMode(project).withDocumentsCommitted(project).submit(NonUrgentExecutor.getInstance())
    }

    private fun notifyRefreshInsightsTaskStarted(fileUri: String?) {
        Log.log(logger::debug, "Notifying RefreshInsightsTaskStarted for {}. ", fileUri)
        if (project.isDisposed) {
            return
        }
        project.messageBus.syncPublisher(RefreshInsightsTaskScheduled.REFRESH_INSIGHTS_TASK_TOPIC).refreshInsightsTaskStarted()
    }

    private fun notifyRefreshInsightsTaskFinished(fileUri: String?) {
        Log.log(logger::debug, "Notifying RefreshInsightsTaskFinished for {}. ", fileUri)
        if (project.isDisposed) {
            return
        }
        project.messageBus.syncPublisher(RefreshInsightsTaskScheduled.REFRESH_INSIGHTS_TASK_TOPIC).refreshInsightsTaskFinished()
    }

}
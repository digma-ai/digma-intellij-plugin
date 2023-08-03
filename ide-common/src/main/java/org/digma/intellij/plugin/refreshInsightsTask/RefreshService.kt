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
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.document.DocumentInfoContainer
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.CodeLessSpan
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.model.CodeLessSpanScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.service.ErrorsViewService
import org.digma.intellij.plugin.ui.service.InsightsViewService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

class RefreshService(private val project: Project) {
    private val logger: Logger = Logger.getInstance(RefreshService::class.java)

    private val errorsViewService: ErrorsViewService = project.getService(ErrorsViewService::class.java)
    private val insightsViewService: InsightsViewService = project.getService(InsightsViewService::class.java)
    private val documentInfoService: DocumentInfoService = project.getService(DocumentInfoService::class.java)
    private val refreshInsightsTaskScheduledLock: ReentrantLock = ReentrantLock()
    private val isGeneralRefreshButtonEnabled = AtomicBoolean(true)

    companion object {
        private val logger = Logger.getInstance(RefreshService::class.java)
        @JvmStatic
        fun getInstance(project: Project): RefreshService {
            logger.warn("Getting instance of ${RefreshService::class.simpleName}")
            return project.getService(RefreshService::class.java)
        }
    }

    suspend fun refreshAllForCurrentFile(file: VirtualFile) {
        Log.log(logger::trace, "Automatic refreshAllForCurrentFile started for file = {}", file.name)

        val selectedTextEditor = withUiContext {
            // this code is on the UI thread
            FileEditorManager.getInstance(project).selectedTextEditor
        }
        var scope = insightsViewService.model.scope
        if (scope is MethodScope) {
            val documentInfoContainer = documentInfoService.getDocumentInfoByMethodInfo(scope.getMethodInfo())

            Log.log(logger::trace, "updateInsightsCacheForActiveDocument starts for file = {}", file.name)
            updateInsightsCacheForActiveDocumentAndRefreshViewIfNeeded(selectedTextEditor, documentInfoContainer, scope)
            Log.log(logger::trace, "updateInsightsCacheForActiveDocument finished for file = {}", file.name)
        } else {
            val documentInfoContainer = documentInfoService.getDocumentInfo(file)
            updateInsightsCacheForActiveDocument(selectedTextEditor, documentInfoContainer)

            scope = insightsViewService.model.scope
            if (scope is CodeLessSpanScope){
                val codelessSpan = scope.getSpan()
                updateModels(codelessSpan)
            }
        }
    }

    fun refreshAllInBackground() {
        val selectedTextEditor = FileEditorManager.getInstance(project).selectedTextEditor
        var scope = insightsViewService.model.scope
        val documentInfoContainer =
                if (scope is MethodScope) {
                    documentInfoService.getDocumentInfoByMethodInfo(scope.getMethodInfo())
                } else {
                    documentInfoService.documentInfoOfFocusedFile
                }

        if (isGeneralRefreshButtonEnabled.getAndSet(false)) {
            //updateInsightsCache must run in the background, the use of refreshInsightsTaskScheduledLock makes sure no two threads
            //update InsightsCache at the same time.
            val task = Runnable {
                refreshInsightsTaskScheduledLock.lock()
                Log.log(logger::trace, "Lock acquired for refreshAll to {}. ", documentInfoContainer?.documentInfo?.fileUri)
                try {
                    notifyRefreshInsightsTaskStarted(documentInfoContainer?.documentInfo?.fileUri)
                    if (scope is MethodScope) {
                        updateInsightsCacheForActiveDocumentAndRefreshViewIfNeeded(selectedTextEditor, documentInfoContainer, scope as MethodScope)
                    } else {
                        updateInsightsCacheForActiveDocument(selectedTextEditor, documentInfoContainer)
                        scope = insightsViewService.model.scope
                        if (scope is CodeLessSpanScope){
                            val codelessSpan = (scope as CodeLessSpanScope).getSpan()
                            updateModels(codelessSpan)
                        }
                    }
                } finally {
                    refreshInsightsTaskScheduledLock.unlock()
                    isGeneralRefreshButtonEnabled.set(true)
                    Log.log(logger::trace, "Lock released for refreshAll to {}. ", documentInfoContainer?.documentInfo?.fileUri)
                    notifyRefreshInsightsTaskFinished(documentInfoContainer?.documentInfo?.fileUri)
                }
            }
            Backgroundable.ensureBackground(project, "Refreshing insights", task)
        }
    }

    // returns true if data has changed, else false
    private fun updateInsightsCacheForActiveDocument(selectedTextEditor: Editor?, documentInfoContainer: DocumentInfoContainer?): Boolean {
        val selectedDocument = selectedTextEditor?.document
        if (selectedDocument != null && documentInfoContainer != null) {
            val oldInsights = documentInfoContainer.allMethodWithInsightsMapForCurrentDocument

            documentInfoContainer.updateCache()

            val newInsights = documentInfoContainer.allMethodWithInsightsMapForCurrentDocument

            return checkIfDataChanged(oldInsights, newInsights)
        }
        return false
    }

    private fun updateInsightsCacheForActiveDocumentAndRefreshViewIfNeeded(selectedTextEditor: Editor?, documentInfoContainer: DocumentInfoContainer?, scope: MethodScope) {
        val isDataChanged = updateInsightsCacheForActiveDocument(selectedTextEditor, documentInfoContainer)

        //todo: this will always run because some classes are not data classes and don't implement equals,
        // for example org.digma.intellij.plugin.model.rest.insights.SpanInfo
        //refresh the UI ONLY if newInsights list is different from oldInsights
        if (isDataChanged) {
            Backgroundable.executeOnPooledThread{
                updateModels(scope.getMethodInfo())
                documentInfoService.notifyDocumentInfoChanged(documentInfoContainer!!.psiFile)
            }
        }
    }




    private fun updateModels(methodInfo: MethodInfo){
        insightsViewService.updateInsightsModelFromRefresh(methodInfo)
        errorsViewService.updateErrorsModelFromRefresh(methodInfo)
    }

    private fun updateModels(codelessSpan: CodeLessSpan){
        insightsViewService.updateInsightsModelFromRefresh(codelessSpan)
        errorsViewService.updateErrorsModelFromRefresh(codelessSpan)
    }







    private fun checkIfDataChanged(oldInsights: Map<String, List<CodeObjectInsight>>, newInsights: Map<String, List<CodeObjectInsight>>): Boolean {
        // In Kotlin, you can compare two maps for equality using the == operator.
        // The == operator checks if the two maps have the same size, the same keys, and the same values for each key.
        return oldInsights != newInsights
    }

    private fun notifyRefreshInsightsTaskStarted(fileUri: String?) {
        Log.log(logger::trace, "Notifying RefreshInsightsTaskStarted for {}. ", fileUri)
        if (project.isDisposed) {
            return
        }
        project.messageBus.syncPublisher(RefreshInsightsTaskScheduled.REFRESH_INSIGHTS_TASK_TOPIC).refreshInsightsTaskStarted()
    }

    private fun notifyRefreshInsightsTaskFinished(fileUri: String?) {
        Log.log(logger::trace, "Notifying RefreshInsightsTaskFinished for {}. ", fileUri)
        if (project.isDisposed) {
            return
        }
        project.messageBus.syncPublisher(RefreshInsightsTaskScheduled.REFRESH_INSIGHTS_TASK_TOPIC).refreshInsightsTaskFinished()
    }

}
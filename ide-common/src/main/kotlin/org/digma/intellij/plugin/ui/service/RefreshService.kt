package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.ui.model.MethodScope

class RefreshService(private val project: Project) {

    private val errorsViewService: ErrorsViewService = project.getService(ErrorsViewService::class.java)
    private val insightsViewService: InsightsViewService = project.getService(InsightsViewService::class.java)

    companion object {
        fun getInstance(project: Project): RefreshService {
            return project.getService(RefreshService::class.java)
        }
    }

    fun refreshAll() {
        val scope = insightsViewService.model.scope
        val selectedTextEditor = FileEditorManager.getInstance(project).selectedTextEditor
        if (scope is MethodScope) {
            val documentInfoContainer = DocumentInfoService.getInstance(project).getDocumentInfoByMethodInfo(scope.getMethodInfo())

            Backgroundable.ensureBackground(project, "Refreshing insights") {
                val selectedDocument = selectedTextEditor?.document

                if (selectedDocument != null) {
                    documentInfoContainer?.updateCache()
                }
                insightsViewService.updateInsightsModel(scope.getMethodInfo())
                errorsViewService.updateErrorsModel(scope.getMethodInfo())

                // update all our local cache in the background
                if (selectedDocument != null) {
                    DocumentInfoService.getInstance(project).updateCacheForOtherOpenedDocuments(documentInfoContainer?.documentInfo?.fileUri)
                } else {
                    DocumentInfoService.getInstance(project).updateCacheForAllOpenedDocuments()
                }
            }
        }
    }
}
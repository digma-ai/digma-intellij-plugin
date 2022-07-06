package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.document.DocumentInfoContainer
import org.digma.intellij.plugin.errors.ErrorsProvider
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.ui.model.DocumentScope
import org.digma.intellij.plugin.ui.model.EmptyScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.errors.ErrorDetailsModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsTabCard
import java.util.*

class ErrorsViewService(project: Project): AbstractViewService(project) {

    //ErrorsModel is singleton object
    private var model = ErrorsModel

    private val errorsProvider: ErrorsProvider = project.getService(ErrorsProvider::class.java)


    override fun getViewDisplayName(): String? {
        return "Errors" + if(model.errorsCount > 0) " (${model.count()})" else ""
    }

    fun contextChanged(
        methodInfo: MethodInfo
    ) {
        val errorsListContainer = errorsProvider.getErrors(methodInfo)
        model.listViewItems = errorsListContainer.listViewItems
        model.scope = MethodScope(methodInfo)
        model.card = ErrorsTabCard.ERRORS_LIST
        model.errorsCount = errorsListContainer.count

        updateUi()
    }



    fun contextChangeNoMethodInfo(dummy: MethodInfo) {
        model.listViewItems = ArrayList()
        model.scope = MethodScope(dummy)
        model.card = ErrorsTabCard.ERRORS_LIST
        model.errorsCount = 0

        updateUi()
    }



    fun showErrorDetails(uid: String) {
        val errorDetails = errorsProvider.getErrorDetails(uid)
        errorDetails.flowStacks.isWorkspaceOnly = project.getService(PersistenceService::class.java).state.isWorkspaceOnly
        model.errorDetails = errorDetails
        model.card = ErrorsTabCard.ERROR_DETAILS

        updateUi()

    }


    fun closeErrorDetails() {
        model.errorDetails = createEmptyErrorDetails(model)
        model.card = ErrorsTabCard.ERRORS_LIST
        updateUi()
    }

    fun empty() {
        model.listViewItems = Collections.emptyList()
        model.scope = EmptyScope("")
        model.card = ErrorsTabCard.ERRORS_LIST
        model.errorsCount = 0

        updateUi()
    }


    /*
    the errors tab don't need to load anything when showing preview in insights tab,
    just needs to update the scope and empty the list
     */
    fun showDocumentPreviewList(documentInfoContainer: DocumentInfoContainer?,
                                fileUri: String) {

        if (documentInfoContainer == null) {
            model.scope = EmptyScope(fileUri.substringAfterLast('/'))
            model.errorsCount = 0
        } else {
            model.scope = DocumentScope(documentInfoContainer.documentInfo)
            model.errorsCount = computeErrorsPreviewCount(documentInfoContainer)
        }

        model.listViewItems = ArrayList()
        model.card = ErrorsTabCard.ERRORS_LIST

        updateUi()
    }


    private fun computeErrorsPreviewCount(documentInfoContainer: DocumentInfoContainer): Int {
        return documentInfoContainer.allSummaries.stream().mapToInt { it.errorsCount }.sum()
    }

    private fun createEmptyErrorDetails(model: ErrorsModel): ErrorDetailsModel {
        val emptyErrorDetails = ErrorDetailsModel()
        emptyErrorDetails.flowStacks.isWorkspaceOnly =
            project.getService(PersistenceService::class.java).state.isWorkspaceOnly
        return emptyErrorDetails
    }


}
package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.document.DocumentInfoContainer
import org.digma.intellij.plugin.errors.ErrorsProvider
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.Models.Empties.EmptyUsageStatusResult
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.ui.model.DocumentScope
import org.digma.intellij.plugin.ui.model.EmptyScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.errors.ErrorDetailsModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsPreviewListItem
import org.digma.intellij.plugin.ui.model.errors.ErrorsTabCard
import java.util.Collections
import java.util.concurrent.locks.ReentrantLock

class ErrorsViewService(project: Project) : AbstractViewService(project) {

    private val logger: Logger = Logger.getInstance(ErrorsViewService::class.java)
    private val lock: ReentrantLock = ReentrantLock()

    //the model is single per the life of an open project in intellij. it shouldn't be created
    //elsewhere in the program. it can not be singleton.
    val model = ErrorsModel()

    private val errorsProvider: ErrorsProvider = project.getService(ErrorsProvider::class.java)

    companion object {
        fun getInstance(project: Project): ErrorsViewService {
            return project.getService(ErrorsViewService::class.java)
        }
    }


    override fun getViewDisplayName(): String {
        return "Errors" + if (model.errorsCount > 0) " (${model.count()})" else ""
    }

    fun updateErrorsModel(
        methodInfo: MethodInfo
    ) {
        lock.lock()
        Log.log(logger::debug, "Lock acquired for contextChanged to {}. ", methodInfo)
        try {
            Log.log(logger::debug, "contextChanged to {}. ", methodInfo)

            val errorsListContainer = errorsProvider.getErrors(methodInfo)

            model.listViewItems = errorsListContainer.listViewItems ?: listOf()
            model.previewListViewItems = ArrayList()
            model.usageStatusResult = errorsListContainer.usageStatus ?: EmptyUsageStatusResult
            model.scope = MethodScope(methodInfo)
            model.card = ErrorsTabCard.ERRORS_LIST
            model.errorsCount = errorsListContainer.count

            updateUi()
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
                Log.log(logger::debug, "Lock released for contextChanged to {}. ", methodInfo)
            }
        }
    }


    fun contextChangeNoMethodInfo(dummy: MethodInfo) {

        Log.log(logger::debug, "contextChangeNoMethodInfo to {}. ", dummy)

        model.listViewItems = ArrayList()
        model.previewListViewItems = ArrayList()
        model.usageStatusResult = EmptyUsageStatusResult
        model.scope = MethodScope(dummy)
        model.card = ErrorsTabCard.ERRORS_LIST
        model.errorsCount = 0

        updateUi()
    }

    fun showErrorList() {
        tabsHelper.notifyTabChanged(1)
    }

    fun showErrorDetails(uid: String) {

        Log.log(logger::debug, "showDocumentPreviewList for {}. ", uid)

        val errorDetails = errorsProvider.getErrorDetails(uid)
        errorDetails.flowStacks.isWorkspaceOnly = PersistenceService.getInstance().state.isWorkspaceOnly
        model.errorDetails = errorDetails
        model.card = ErrorsTabCard.ERROR_DETAILS

        updateUi()

    }


    fun closeErrorDetails() {

        Log.log(logger::debug, "closeErrorDetails called")

        model.errorDetails = createEmptyErrorDetails()
        model.card = ErrorsTabCard.ERRORS_LIST
        updateUi()
    }


    /**
     * empty should be called only when there is no file opened in the editor and not in
     * any other case.
     */
    fun empty() {

        Log.log(logger::debug, "empty called")

        model.listViewItems = Collections.emptyList()
        model.previewListViewItems = ArrayList()
        model.usageStatusResult = EmptyUsageStatusResult
        model.scope = EmptyScope("")
        model.card = ErrorsTabCard.ERRORS_LIST
        model.errorsCount = 0

        updateUi()
    }

    fun emptyNonSupportedFile(fileUri: String) {

        Log.log(logger::debug, "emptyNonSupportedFile called")

        model.listViewItems = Collections.emptyList()
        model.previewListViewItems = ArrayList()
        model.usageStatusResult = EmptyUsageStatusResult
        model.scope = EmptyScope(getNonSupportedFileScopeMessage(fileUri))
        model.card = ErrorsTabCard.ERRORS_LIST
        model.errorsCount = 0

        updateUi()
    }



    fun showDocumentPreviewList(
        documentInfoContainer: DocumentInfoContainer?,
        fileUri: String
    ) {

        Log.log(logger::debug, "showDocumentPreviewList for {}. ", fileUri)

        if (documentInfoContainer == null) {
            model.scope = EmptyScope(fileUri.substringAfterLast('/'))
            model.listViewItems = ArrayList()
            model.errorsCount = 0
            model.usageStatusResult = EmptyUsageStatusResult
            model.previewListViewItems = ArrayList()
            model.card = ErrorsTabCard.PREVIEW_LIST
        } else {
            model.scope = DocumentScope(documentInfoContainer.documentInfo)
            model.listViewItems = ArrayList()
            model.errorsCount = computeErrorsPreviewCount(documentInfoContainer)
            model.usageStatusResult = documentInfoContainer.usageStatusOfErrors
            model.previewListViewItems = getDocumentPreviewItems(documentInfoContainer)
            model.card = ErrorsTabCard.PREVIEW_LIST
        }

        updateUi()
    }



    private fun getDocumentPreviewItems(documentInfoContainer: DocumentInfoContainer): List<ErrorsPreviewListItem> {

        val listViewItems = ArrayList<ErrorsPreviewListItem>()
        documentInfoContainer.documentInfo.methods.forEach { (id, methodInfo) ->
            listViewItems.add(ErrorsPreviewListItem(methodInfo.id, documentInfoContainer.hasErrors(id), methodInfo.getRelatedCodeObjectIdsWithType().any()))
        }

        //sort by name of the function, it will be sorted later by sortIndex when added to a PanelListModel, but
        // because they all have the same sortIndex then positions will not change
        Collections.sort(listViewItems, Comparator.comparing { it.name })
        return listViewItems

    }



    private fun computeErrorsPreviewCount(documentInfoContainer: DocumentInfoContainer): Int {
        return documentInfoContainer.countInsightsByType(InsightType.Errors)
    }

    private fun createEmptyErrorDetails(): ErrorDetailsModel {
        val emptyErrorDetails = ErrorDetailsModel()
        emptyErrorDetails.flowStacks.isWorkspaceOnly = PersistenceService.getInstance().state.isWorkspaceOnly
        return emptyErrorDetails
    }

    fun refreshErrorsModel() {
        val scope = model.scope
        if (scope is MethodScope) {
            Backgroundable.ensureBackground(project, "Refresh errors list") {
                updateErrorsModel(scope.getMethodInfo())
            }
        }
    }

}
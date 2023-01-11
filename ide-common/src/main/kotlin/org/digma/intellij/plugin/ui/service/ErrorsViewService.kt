package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.document.DocumentInfoContainer
import org.digma.intellij.plugin.errors.ErrorsProvider
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.Models.Empties.EmptyUsageStatusResult
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.ui.model.DocumentScope
import org.digma.intellij.plugin.ui.model.EmptyScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.errors.ErrorDetailsModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsTabCard
import java.util.*
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

            model.listViewItems = errorsListContainer.listViewItems
            model.usageStatusResult = errorsListContainer.usageStatus
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
        model.usageStatusResult = EmptyUsageStatusResult
        model.scope = MethodScope(dummy)
        model.card = ErrorsTabCard.ERRORS_LIST
        model.errorsCount = 0

        updateUi()
    }


    fun showErrorDetails(uid: String) {

        Log.log(logger::debug, "showDocumentPreviewList for {}. ", uid)

        val errorDetails = errorsProvider.getErrorDetails(uid)
        errorDetails.flowStacks.isWorkspaceOnly =
            project.getService(PersistenceService::class.java).state.isWorkspaceOnly
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

    fun empty() {

        Log.log(logger::debug, "empty called")

        model.listViewItems = Collections.emptyList()
        model.usageStatusResult = EmptyUsageStatusResult
        model.scope = EmptyScope("")
        model.card = ErrorsTabCard.ERRORS_LIST
        model.errorsCount = 0

        updateUi()
    }

    fun emptyNonSupportedFile(fileUri: String) {

        Log.log(logger::debug, "emptyNonSupportedFile called")

        model.listViewItems = Collections.emptyList()
        model.usageStatusResult = EmptyUsageStatusResult
        model.scope = EmptyScope(getNonSupportedFileScopeMessage(fileUri))
        model.card = ErrorsTabCard.ERRORS_LIST
        model.errorsCount = 0

        updateUi()
    }


    /*
    the errors tab don't need to load anything when showing preview in insights tab,
    just needs to update the scope and empty the list
     */
    fun showDocumentPreviewList(
        documentInfoContainer: DocumentInfoContainer?,
        fileUri: String
    ) {

        Log.log(logger::debug, "showDocumentPreviewList for {}. ", fileUri)

        if (documentInfoContainer == null) {
            model.scope = EmptyScope(fileUri.substringAfterLast('/'))
            model.errorsCount = 0
            model.usageStatusResult = EmptyUsageStatusResult
        } else {
            model.scope = DocumentScope(documentInfoContainer.documentInfo)
            model.errorsCount = computeErrorsPreviewCount(documentInfoContainer)
            model.usageStatusResult = documentInfoContainer.usageStatusOfErrors
        }

        model.listViewItems = ArrayList()
        model.card = ErrorsTabCard.ERRORS_LIST

        updateUi()
    }


    private fun computeErrorsPreviewCount(documentInfoContainer: DocumentInfoContainer): Int {
        return documentInfoContainer.allInsights.stream().filter{ it is ErrorInsight }.count().toInt()
    }

    private fun createEmptyErrorDetails(): ErrorDetailsModel {
        val emptyErrorDetails = ErrorDetailsModel()
        emptyErrorDetails.flowStacks.isWorkspaceOnly =
            project.getService(PersistenceService::class.java).state.isWorkspaceOnly
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
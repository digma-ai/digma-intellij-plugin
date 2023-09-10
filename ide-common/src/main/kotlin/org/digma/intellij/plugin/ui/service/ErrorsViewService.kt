package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.apache.commons.lang.builder.EqualsBuilder
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.document.DocumentInfoContainer
import org.digma.intellij.plugin.errors.ErrorsListContainer
import org.digma.intellij.plugin.errors.ErrorsProvider
import org.digma.intellij.plugin.insights.CodeLessSpanInsightsProvider
import org.digma.intellij.plugin.insights.CodelessSpanErrorsContainer
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.Models.Empties.EmptyUsageStatusResult
import org.digma.intellij.plugin.model.discovery.CodeLessSpan
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.navigation.InsightsAndErrorsTabsHelper
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.model.CodeLessSpanScope
import org.digma.intellij.plugin.ui.model.DocumentScope
import org.digma.intellij.plugin.ui.model.EmptyScope
import org.digma.intellij.plugin.ui.model.ErrorDetailsScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.UIInsightsStatus
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


    companion object {
        @JvmStatic
        fun getInstance(project: Project): ErrorsViewService {
            return project.getService(ErrorsViewService::class.java)
        }
    }


    override fun getViewDisplayName(): String {
        return "Errors" + if (model.errorsCount > 0) " (${model.count()})" else ""
    }



    //todo: quick and dirty prevent race condition with refresh task until we have time to re-write it
    fun updateErrorsModelFromRefresh(codeLessSpan: CodeLessSpan) {
        lock.lock()
        try {
            //don't let the refresh task update if it's not the same CodeLessSpan
            if ( model.scope is CodeLessSpanScope && codeLessSpan == (model.scope as CodeLessSpanScope).getSpan()) {
                updateErrorsModelImpl(codeLessSpan)
            }
        }finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }

    //todo: quick and dirty prevent race condition with refresh task until we have time to re-write it
    fun updateErrorsModel(codeLessSpan: CodeLessSpan) {
        lock.lock()
        try {
            updateErrorsModelImpl(codeLessSpan)
        }finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }


    private fun updateErrorsModelImpl(codeLessSpan: CodeLessSpan) {

        project.service<MainToolWindowCardsController>().showMainPanel()

        val codeLessInsightsProvider = CodeLessSpanInsightsProvider(codeLessSpan,project)

        Log.log(logger::debug, "updateInsightsModel to {}. ", codeLessSpan)

        val codelessSpanErrorsContainer: CodelessSpanErrorsContainer? = codeLessInsightsProvider.getErrors()

        val errorsListContainer: ErrorsListContainer? = codelessSpanErrorsContainer?.errorsContainer

        if (errorsListContainer?.listViewItems.isNullOrEmpty()){
            Log.log(logger::debug,project, "could not load errors for {}, see logs for details",codeLessSpan )
        }

        //todo: this is temporary, flickering happens because the UI is rebuilt on every refresh, when
        // UI components are changed to bind to models flickering should not happen and we can just
        // update the UI even with same data, it should be faster and more correct then deep equals of the data.
        //this is the way to prevent updating the UI if insights list didn't change between refresh
        // and by the way prevent flickering
        //kotlin equality doesn't work for listViewItems because ListViewItem does not implement equals
        // so only the expensive reflectionEquals works here
        if (model.scope is CodeLessSpanScope &&
            (model.scope as CodeLessSpanScope).getSpan() == codeLessSpan &&
            EqualsBuilder.reflectionEquals(errorsListContainer?.listViewItems,model.listViewItems)) {
            return
        }


        model.listViewItems = errorsListContainer?.listViewItems ?: listOf()
        model.previewListViewItems = ArrayList()
        model.usageStatusResult = errorsListContainer?.usageStatus ?: EmptyUsageStatusResult
        model.scope = CodeLessSpanScope(codeLessSpan, codelessSpanErrorsContainer?.insightsResponse?.spanInfo)
        model.card = ErrorsTabCard.ERRORS_LIST
        model.errorsCount = errorsListContainer?.count ?: 0

        updateUi()
    }


    //todo: quick and dirty prevent race condition with refresh task until we have time to re-write it
    fun updateErrorsModelFromRefresh(methodInfo: MethodInfo) {
        lock.lock()
        try {
            //don't let the refresh task update if it's not the same methodInfo
            if ( model.scope is MethodScope && methodInfo == (model.scope as MethodScope).getMethodInfo()) {
                updateErrorsModelImpl(methodInfo)
            }
        }finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }

    //todo: quick and dirty prevent race condition with refresh task until we have time to re-write it
    fun updateErrorsModel(methodInfo: MethodInfo) {
        lock.lock()
        try {
            updateErrorsModelImpl(methodInfo)
        }finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }


    private fun updateErrorsModelImpl(methodInfo: MethodInfo) {

        val errorsProvider: ErrorsProvider = project.getService(ErrorsProvider::class.java)
        updateErrorsModelWithErrorsProvider(methodInfo,errorsProvider)
    }

    private fun updateErrorsModelWithErrorsProvider(methodInfo: MethodInfo, errorsProvider: ErrorsProvider) {
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
        project.service<InsightsAndErrorsTabsHelper>().switchToErrorsTab()
    }

    fun showErrorDetails(uid: String, errorsProvider: ErrorsProvider,replaceScope: Boolean) {

        Log.log(logger::debug, "showDocumentPreviewList for {}. ", uid)

        val errorDetails = errorsProvider.getErrorDetails(uid)
        errorDetails.flowStacks.isWorkspaceOnly = PersistenceService.getInstance().state.isWorkspaceOnly
        model.errorDetails = errorDetails
        model.card = ErrorsTabCard.ERROR_DETAILS

        if (replaceScope) {
            project.service<InsightsViewService>().model.scope = ErrorDetailsScope(errorDetails.getName())
            project.service<InsightsViewService>().model.status = UIInsightsStatus.Default
        }

        updateUi()

    }


    override fun canUpdateUI(): Boolean {
        return !project.service<InsightsAndErrorsTabsHelper>().isErrorDetailsOn()
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
    @Deprecated("for removal")
    fun empty() {

    //Note: we do not empty the model anymore

//        Log.log(logger::debug, "empty called")
//
//        model.listViewItems = Collections.emptyList()
//        model.previewListViewItems = ArrayList()
//        model.usageStatusResult = EmptyUsageStatusResult
//        model.scope = EmptyScope("")
//        model.card = ErrorsTabCard.ERRORS_LIST
//        model.errorsCount = 0
//
//        updateUi()
    }

    @Deprecated("for removal")
    fun emptyNonSupportedFile(fileUri: String) {

        //Note: we do not empty the model anymore

//        Log.log(logger::debug, "emptyNonSupportedFile called")
//
//        model.listViewItems = Collections.emptyList()
//        model.previewListViewItems = ArrayList()
//        model.usageStatusResult = EmptyUsageStatusResult
//        model.scope = EmptyScope(getNonSupportedFileScopeMessage(fileUri))
//        model.card = ErrorsTabCard.ERRORS_LIST
//        model.errorsCount = 0
//
//        updateUi()
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
            listViewItems.add(ErrorsPreviewListItem(methodInfo.id, documentInfoContainer.hasErrors(id), methodInfo.getRelatedCodeObjectIds().any()))
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
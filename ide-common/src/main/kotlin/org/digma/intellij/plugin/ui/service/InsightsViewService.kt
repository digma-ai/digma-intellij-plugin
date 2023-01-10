package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.document.DocumentInfoContainer
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.insights.InsightsListContainer
import org.digma.intellij.plugin.insights.InsightsProvider
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.Models.Empties.EmptyUsageStatusResult
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.discovery.SpanInfo
import org.digma.intellij.plugin.ui.model.DocumentScope
import org.digma.intellij.plugin.ui.model.EmptyScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import org.digma.intellij.plugin.ui.model.insights.InsightsTabCard
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import java.util.stream.Collectors

class InsightsViewService(project: Project) : AbstractViewService(project) {

    private val logger: Logger = Logger.getInstance(InsightsViewService::class.java)
    private val lock: ReentrantLock = ReentrantLock()

    //the model is single per the life of an open project in intellij. it shouldn't be created
    //elsewhere in the program. it can not be singleton.
    val model = InsightsModel()

    private val insightsProvider: InsightsProvider = project.getService(InsightsProvider::class.java)


    override fun getViewDisplayName(): String {
        return "Insights" + if (model.insightsCount > 0) " (${model.count()})" else ""
    }


    companion object {
        fun getInstance(project: Project): InsightsViewService {
            return project.getService(InsightsViewService::class.java)
        }
    }

    fun updateInsightsModel(
        methodInfo: MethodInfo
    ) {
        lock.lock()
        Log.log(logger::debug, "Lock acquired for updateInsightsModel to {}. ", methodInfo)
        try {
            Log.log(logger::debug, "updateInsightsModel to {}. ", methodInfo)

            val insightsListContainer = insightsProvider.getCachedInsights(methodInfo)

            model.listViewItems = insightsListContainer.listViewItems
            model.previewListViewItems = ArrayList()
            model.usageStatusResult = insightsListContainer.usageStatus
            model.scope = MethodScope(methodInfo)
            model.insightsCount = insightsListContainer.count
            model.card = InsightsTabCard.INSIGHTS

            updateUi()
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
                Log.log(logger::debug, "Lock released for updateInsightsModel to {}. ", methodInfo)
            }
        }
    }


    fun contextChangeNoMethodInfo(dummy: MethodInfo) {

        Log.log(logger::debug, "contextChangeNoMethodInfo to {}. ", dummy)

        model.listViewItems = ArrayList()
        model.previewListViewItems = ArrayList()
        model.usageStatusResult = EmptyUsageStatusResult
        model.scope = MethodScope(dummy)
        model.insightsCount = 0
        model.card = InsightsTabCard.INSIGHTS

        updateUi()
    }


    fun empty() {

        Log.log(logger::debug, "empty called")

        model.listViewItems = ArrayList()
        model.previewListViewItems = ArrayList()
        model.usageStatusResult = EmptyUsageStatusResult
        model.scope = EmptyScope("")
        model.insightsCount = 0
        model.card = InsightsTabCard.INSIGHTS

        updateUi()
    }

    fun emptyNonSupportedFile(fileUri: String) {

        Log.log(logger::debug, "empty called")

        model.listViewItems = ArrayList()
        model.previewListViewItems = ArrayList()
        model.usageStatusResult = EmptyUsageStatusResult
        model.scope = EmptyScope(getNonSupportedFileScopeMessage(fileUri))
        model.insightsCount = 0
        model.card = InsightsTabCard.INSIGHTS

        updateUi()
    }

    fun showDocumentPreviewList(
        documentInfoContainer: DocumentInfoContainer?,
        fileUri: String
    ) {

        Log.log(logger::debug, "showDocumentPreviewList for {}. ", fileUri)

        if (documentInfoContainer == null) {
            model.previewListViewItems = ArrayList()
            model.usageStatusResult = EmptyUsageStatusResult
            model.scope = EmptyScope(fileUri.substringAfterLast('/'))
            model.insightsCount = 0
        } else {
            model.previewListViewItems = getDocumentPreviewItems(documentInfoContainer)
            model.usageStatusResult = documentInfoContainer.usageStatus
            model.scope = DocumentScope(documentInfoContainer.documentInfo)
            model.insightsCount = computeInsightsPreviewCount(documentInfoContainer)
        }

        model.listViewItems = ArrayList()
        model.card = InsightsTabCard.PREVIEW
        updateUi()
    }


    private fun computeInsightsPreviewCount(documentInfoContainer: DocumentInfoContainer): Int {
        return documentInfoContainer.allInsights.size
    }

    private fun getDocumentPreviewItems(documentInfoContainer: DocumentInfoContainer): List<ListViewItem<String>> {

        val listViewItems = ArrayList<ListViewItem<String>>()
        val docSummariesIds: Set<String> = documentInfoContainer.allInsights.stream().map { it.codeObjectId }.collect(Collectors.toSet())

        documentInfoContainer.documentInfo.methods.forEach { (id, methodInfo) ->
            val ids = methodInfo.spans.stream().map { obj: SpanInfo -> obj.id }
                .collect(Collectors.toList())
            ids.add(id)

            if (docSummariesIds.any { ids.contains(it) }) {
                listViewItems.add(ListViewItem(methodInfo.id, 0))
            }
        }

        //sort by name of the function, it will be sorted later by sortIndex when added to a PanelListModel, but
        // because they all have the same sortIndex then positions will not change
        Collections.sort(listViewItems, Comparator.comparing { it.modelObject })
        return listViewItems

    }

    fun refreshInsightsModel() {
        val scope = model.scope
        if (scope is MethodScope) {
            Backgroundable.ensureBackground(project, "Refresh insights model") {
                updateInsightsModel(scope.getMethodInfo())
            }
        }
    }

}
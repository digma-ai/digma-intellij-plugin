package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.IDEUtilsService
import org.digma.intellij.plugin.common.modelChangeListener.ModelChangeListener
import org.digma.intellij.plugin.document.DocumentInfoContainer
import org.digma.intellij.plugin.insights.InsightsProvider
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.Models.Empties.EmptyUsageStatusResult
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.rest.insights.InsightStatus
import org.digma.intellij.plugin.ui.model.DocumentScope
import org.digma.intellij.plugin.ui.model.EmptyScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.UIInsightsStatus
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import org.digma.intellij.plugin.ui.model.insights.InsightsPreviewListItem
import org.digma.intellij.plugin.ui.model.insights.InsightsTabCard
import org.jetbrains.annotations.VisibleForTesting
import java.util.Collections
import java.util.concurrent.locks.ReentrantLock

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

            model.listViewItems = insightsListContainer.listViewItems ?: listOf()
            model.previewListViewItems = ArrayList()
            model.usageStatusResult = insightsListContainer.usageStatus ?: EmptyUsageStatusResult
            model.scope = MethodScope(methodInfo)
            model.insightsCount = insightsListContainer.count
            model.card = InsightsTabCard.INSIGHTS


            if (model.listViewItems.isNotEmpty()) {
                model.status = UIInsightsStatus.Default
            } else {
                model.status = UIInsightsStatus.Loading
                Log.log(logger::debug,"No insights for method {}, Starting background thread.",methodInfo.name)
                Backgroundable.runInNewBackgroundThread(project,"Fetching insights status for method ${methodInfo.name}"){

                    Log.log(logger::debug,"Loading backend status in background for method {}",methodInfo.name)
                    val insightStatus = insightsProvider.getInsightStatus(methodInfo)
                    Log.log(logger::debug,"Got status from backend {} for method {}",insightStatus,methodInfo.name)
                    //if status is null assign EmptyStatus, it probably means there was a communication error
                    model.status = insightStatus?.let {
                        return@let toUiInsightStatus(it, methodInfo.hasRelatedCodeObjectIds())
                    } ?: UIInsightsStatus.NoInsights

                    Log.log(logger::debug,"UIInsightsStatus for method {} is {}",methodInfo.name,model.status)

                    notifyModelChangedAndUpdateUi()
                }
            }

            notifyModelChangedAndUpdateUi()

        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
                Log.log(logger::debug, "Lock released for updateInsightsModel to {}. ", methodInfo)
            }
        }
    }


    @VisibleForTesting
    fun toUiInsightStatus(status: InsightStatus, methodHasRelatedCodeObjectIds: Boolean): UIInsightsStatus {
        //no need for else branch, all possible values are handled
        return when (status) {
            InsightStatus.InsightExist -> UIInsightsStatus.InsightPending
            InsightStatus.InsightPending -> UIInsightsStatus.InsightPending
            InsightStatus.NoSpanData -> {
                if (methodHasRelatedCodeObjectIds)
                    return UIInsightsStatus.NoSpanData // the client(this plugin) is aware of code objects, but server is not (yet)
                else
                    if (IDEUtilsService.getInstance(project).isJavaProject)
                        return UIInsightsStatus.NoObservability
                    else
                        return UIInsightsStatus.NoInsights
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
        model.status = UIInsightsStatus.NoInsights

        notifyModelChangedAndUpdateUi()

    }


    /**
     * empty should be called only when there is no file opened in the editor and not in
     * any other case.
     */
    fun empty() {

        Log.log(logger::debug, "empty called")

        model.listViewItems = ArrayList()
        model.previewListViewItems = ArrayList()
        model.usageStatusResult = EmptyUsageStatusResult
        model.scope = EmptyScope("")
        model.insightsCount = 0
        model.card = InsightsTabCard.INSIGHTS
        //when empty set Default status, empty editor should be covered by MainToolWindowCardsController
        model.status = UIInsightsStatus.Default

        notifyModelChangedAndUpdateUi()

    }

    fun emptyNonSupportedFile(fileUri: String) {

        Log.log(logger::debug, "empty called")

        model.listViewItems = ArrayList()
        model.previewListViewItems = ArrayList()
        model.usageStatusResult = EmptyUsageStatusResult
        model.scope = EmptyScope(getNonSupportedFileScopeMessage(fileUri))
        model.insightsCount = 0
        model.card = InsightsTabCard.INSIGHTS
        //when non supported file set Default status, non-supported file should be covered by MainToolWindowCardsController
        model.status = UIInsightsStatus.Default

        notifyModelChangedAndUpdateUi()

    }

    fun showDocumentPreviewList(
            documentInfoContainer: DocumentInfoContainer?,
            fileUri: String
    ) {

        Log.log(logger::debug, "showDocumentPreviewList for {}. ", fileUri)

        if (documentInfoContainer == null) {
            model.previewListViewItems = ArrayList()
            model.listViewItems = ArrayList()
            model.usageStatusResult = EmptyUsageStatusResult
            model.scope = EmptyScope(fileUri.substringAfterLast('/'))
            model.insightsCount = 0
            model.card = InsightsTabCard.PREVIEW
            model.status = UIInsightsStatus.NoInsights
        } else {
            model.previewListViewItems = getDocumentPreviewItems(documentInfoContainer)
            model.listViewItems = ArrayList()
            model.usageStatusResult = documentInfoContainer.usageStatus
            model.scope = DocumentScope(documentInfoContainer.documentInfo)
            model.insightsCount = computeInsightsPreviewCount(documentInfoContainer)
            model.card = InsightsTabCard.PREVIEW
            model.status = UIInsightsStatus.Default
            if (!model.hasInsights()) {
                if(model.hasDiscoverableCodeObjects()){
                    model.status = UIInsightsStatus.NoSpanData
                }
                else{
                    model.status = UIInsightsStatus.NoInsights
                }
            }
        }

        notifyModelChangedAndUpdateUi()

    }


    private fun computeInsightsPreviewCount(documentInfoContainer: DocumentInfoContainer): Int {
        return documentInfoContainer.insightsCount
    }

    private fun getDocumentPreviewItems(documentInfoContainer: DocumentInfoContainer): List<InsightsPreviewListItem> {

        val listViewItems = ArrayList<InsightsPreviewListItem>()
        documentInfoContainer.documentInfo.methods.forEach { (id, methodInfo) ->
            listViewItems.add(InsightsPreviewListItem(methodInfo.id, documentInfoContainer.hasInsights(id), methodInfo.getRelatedCodeObjectIds().any()))
        }

        //sort by name of the function, it will be sorted later by sortIndex when added to a PanelListModel, but
        // because they all have the same sortIndex then positions will not change
        Collections.sort(listViewItems, Comparator.comparing { it.name })
        return listViewItems

    }

    fun refreshInsightsModel() {
        val scope = model.scope
        if (scope is MethodScope) {
            Backgroundable.ensureBackground(project, "Refresh insights list") {
                updateInsightsModel(scope.getMethodInfo())
            }
        }
    }

    private fun notifyModelChanged() {
        Log.log(logger::debug, "Firing ModelChange event for {}", model)
        if (project.isDisposed) {
            return
        }
        val publisher = project.messageBus.syncPublisher(ModelChangeListener.MODEL_CHANGED_TOPIC)
        publisher.modelChanged(model)
    }

    private fun notifyModelChangedAndUpdateUi() {
        notifyModelChanged()
        updateUi()
    }

}
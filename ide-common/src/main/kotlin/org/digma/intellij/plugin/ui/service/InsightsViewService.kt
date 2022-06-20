package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.Content
import org.digma.intellij.plugin.document.DocumentInfoContainer
import org.digma.intellij.plugin.insights.InsightsListContainer
import org.digma.intellij.plugin.insights.InsightsProvider
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.discovery.SpanInfo
import org.digma.intellij.plugin.ui.model.DocumentScope
import org.digma.intellij.plugin.ui.model.EmptyScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsTabCard
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import org.digma.intellij.plugin.ui.model.insights.InsightsTabCard
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import java.util.stream.Collectors

class InsightsViewService(val project: Project): AbstractViewService() {

    //InsightsModel is singleton object
    private var model = InsightsModel

    //these may be null if the tool window did not open yet
    var panel: DigmaTabPanel? = null
    var toolWindow: ToolWindow? = null
    var insightsContent: Content? = null

    private val insightsProvider: InsightsProvider = project.getService(InsightsProvider::class.java)


    fun contextChanged(
        methodInfo: MethodInfo
    ) {

        val insightsListContainer: InsightsListContainer = insightsProvider.getInsights(methodInfo)

        model.listViewItems = insightsListContainer.listViewItems
        model.previewListViewItems = ArrayList()
        model.scope = MethodScope(methodInfo)
        model.insightsCount = insightsListContainer.count
        model.card = InsightsTabCard.INSIGHTS

        updateUi()
    }


    fun contextChangeNoMethodInfo(dummy: MethodInfo) {
        model.listViewItems = ArrayList()
        model.previewListViewItems = ArrayList()
        model.scope = MethodScope(dummy)
        model.insightsCount = 0
        model.card = InsightsTabCard.INSIGHTS

        updateUi()
    }



    fun empty() {
        model.listViewItems = ArrayList()
        model.previewListViewItems = ArrayList()
        model.scope = EmptyScope("")
        model.insightsCount = 0
        model.card = InsightsTabCard.INSIGHTS

        updateUi()
    }

    fun showDocumentPreviewList(documentInfoContainer: DocumentInfoContainer?,
                                fileUri: String) {

        if (documentInfoContainer == null) {
            model.previewListViewItems = ArrayList()
            model.scope = EmptyScope(fileUri.substringAfterLast('/'))
            model.insightsCount = 0
        } else {
            model.previewListViewItems = getDocumentPreviewItems(documentInfoContainer)
            model.scope = DocumentScope(documentInfoContainer.documentInfo)
            model.insightsCount = computeInsightsPreviewCount(documentInfoContainer)
        }

        model.listViewItems = ArrayList()
        model.card = InsightsTabCard.PREVIEW
        updateUi()
        setVisible()
    }



    private fun computeInsightsPreviewCount(documentInfoContainer: DocumentInfoContainer): Int {
        return documentInfoContainer.allSummaries.stream().mapToInt { it.insightsCount }.sum()
    }

    private fun getDocumentPreviewItems(documentInfoContainer: DocumentInfoContainer): List<ListViewItem<String>> {

        val listViewItems = ArrayList<ListViewItem<String>>()
        val docSummariesIds: Set<String> = documentInfoContainer.allSummaries.stream().map { it.codeObjectId }.collect(Collectors.toSet())

        documentInfoContainer.documentInfo.methods.forEach { (id, methodInfo) ->
            //todo: we probably don't need to check for spans code object id because the span summary code object id is the method id
            val ids = methodInfo.spans.stream().map { obj: SpanInfo -> obj.id }
                .collect(Collectors.toList())
            ids.add(id)

            if (docSummariesIds.any { ids.contains(it) }) {
                listViewItems.add(ListViewItem(methodInfo.id,0))
            }
        }

        return listViewItems

    }


    fun setVisible() {
        toolWindow?.contentManager?.setSelectedContent(insightsContent!!, true)
    }

    fun isVisible():Boolean{
        return toolWindow?.contentManager?.selectedContent === insightsContent
    }

    fun setContent(toolWindow: ToolWindow, insightsContent: Content) {
        this.toolWindow = toolWindow
        this.insightsContent = insightsContent
    }


    override fun updateUi(){
        panel?.reset()
    }
}
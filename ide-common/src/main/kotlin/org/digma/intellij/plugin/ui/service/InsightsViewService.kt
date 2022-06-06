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
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import org.digma.intellij.plugin.ui.model.insights.InsightsTabCard
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import java.util.stream.Collectors

class InsightsViewService(val project: Project) {

    lateinit var panel: DigmaTabPanel
    lateinit var model: InsightsModel
    lateinit var toolWindow: ToolWindow
    lateinit var insightsContent: Content
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

        panel.reset()
    }


    fun contextChangeNoMethodInfo(dummy: MethodInfo) {
        model.listViewItems = ArrayList()
        model.previewListViewItems = ArrayList()
        model.scope = MethodScope(dummy)
        model.insightsCount = 0
        model.card = InsightsTabCard.INSIGHTS

        panel.reset()
    }



    fun empty() {
        model.listViewItems = ArrayList()
        model.previewListViewItems = ArrayList()
        model.scope = EmptyScope("")
        model.insightsCount = 0
        model.card = InsightsTabCard.INSIGHTS

        panel.reset()
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
        panel.reset()
        setVisible()
    }



    private fun computeInsightsPreviewCount(documentInfoContainer: DocumentInfoContainer): Int {
        return documentInfoContainer.summaries.values.stream().mapToInt { it.insightsCount }.sum()
    }

    private fun getDocumentPreviewItems(documentInfoContainer: DocumentInfoContainer): List<ListViewItem<String>> {

        val listViewItems = ArrayList<ListViewItem<String>>()

        documentInfoContainer.documentInfo.methods.forEach { (id, methodInfo) ->

            val ids = methodInfo.spans.stream().map { obj: SpanInfo -> obj.id }
                .collect(Collectors.toList())
            ids.add(id)

            if (documentInfoContainer.summaries.keys.any { ids.contains(it) }) {
                listViewItems.add(ListViewItem(methodInfo.id,0))
            }
        }

        return listViewItems

    }


    fun setVisible() {
        toolWindow.contentManager.setSelectedContent(insightsContent, true)
    }

    fun setContent(toolWindow: ToolWindow, insightsContent: Content) {
        this.toolWindow = toolWindow
        this.insightsContent = insightsContent
    }

}
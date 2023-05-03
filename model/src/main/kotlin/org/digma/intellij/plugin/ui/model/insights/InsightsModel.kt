package org.digma.intellij.plugin.ui.model.insights

import org.digma.intellij.plugin.model.Models.Empties.EmptyUsageStatusResult
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult
import org.digma.intellij.plugin.ui.model.DocumentScope
import org.digma.intellij.plugin.ui.model.EmptyScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.PanelModel
import org.digma.intellij.plugin.ui.model.Scope
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.Collections

class PreviewListItem(val name: String, val hasInsights: Boolean, val hasRelatedCodeObjects: Boolean )

class InsightsModel : PanelModel {

    var insightsCount: Int = 0
    var listViewItems: List<ListViewItem<*>> = Collections.emptyList()
    var previewListViewItems: List<PreviewListItem> = Collections.emptyList()
    var usageStatusResult: UsageStatusResult = EmptyUsageStatusResult
    var card: InsightsTabCard = InsightsTabCard.INSIGHTS
    var status: UiInsightStatus = UiInsightStatus.NoInsights
    var scope: Scope = EmptyScope("")

    fun getMethodNamesWithInsights(): List<ListViewItem<String>>{
        val methodsWithInsights = mutableListOf<ListViewItem<String>>();

        var index = 0
        previewListViewItems.filter { o -> o.hasInsights }.forEach {
            methodsWithInsights.add(ListViewItem(it.name, index++));
        }
        return methodsWithInsights;
    }
    fun hasInsights(): Boolean{
        return previewListViewItems.any { o->o.hasInsights };
    }

    fun hasDiscoverableCodeObjects(): Boolean{
        return previewListViewItems.any{o->o.hasRelatedCodeObjects}
    }

    override fun count(): String {
        return insightsCount.toString()
    }

    override fun isMethodScope(): Boolean {
        return scope is MethodScope
    }

    override fun isDocumentScope(): Boolean {
        return scope is DocumentScope
    }

    override fun getScope(): String {
        return scope.getScope()
    }

    override fun getScopeTooltip(): String {
        return scope.getScopeTooltip()
    }

    override fun getUsageStatus(): UsageStatusResult {
        return usageStatusResult
    }

    fun getPreviewListMessage(): String {
        if (scope is EmptyScope) {
            return "No code objects found for this document"
        } else {
            return "Try to click one of the following code objects"
        }
    }
}


enum class InsightsTabCard {
    INSIGHTS, PREVIEW
}

enum class UiInsightStatus {
    Unknown, // initial state, when insights is 0, might be changed to something else
    InsightExist, // insight count is greater than 0
    NoInsights,
    LoadingInsights,
    InsightPending, // backend is aware of the code objects, but still no insights, soon there will be
    NoSpanData, // backend is not aware of code objects
    NoObservability, // method has no insights and has no related code object ids (spans and/or endpoints)
}

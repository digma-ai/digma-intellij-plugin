package org.digma.intellij.plugin.ui.model.insights

import org.digma.intellij.plugin.model.Models.Empties.EmptyUsageStatusResult
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult
import org.digma.intellij.plugin.ui.model.DocumentScope
import org.digma.intellij.plugin.ui.model.EmptyScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.PanelModel
import org.digma.intellij.plugin.ui.model.Scope
import org.digma.intellij.plugin.ui.model.UIInsightsStatus
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.Collections

class InsightsPreviewListItem(val name: String, val hasInsights: Boolean, val hasRelatedCodeObjects: Boolean )

enum class InsightsTabCard {
    INSIGHTS, PREVIEW
}

class InsightsModel : PanelModel {

    var insightsCount: Int = 0
    var listViewItems: List<ListViewItem<*>> = Collections.emptyList()
    var previewListViewItems: List<InsightsPreviewListItem> = Collections.emptyList()
    var usageStatusResult: UsageStatusResult = EmptyUsageStatusResult
    var card: InsightsTabCard = InsightsTabCard.INSIGHTS
    var status: UIInsightsStatus = UIInsightsStatus.Default
    var scope: Scope = EmptyScope("")

    fun getMethodNamesWithInsights(): List<ListViewItem<String>>{
        val methodsWithInsights = mutableListOf<ListViewItem<String>>()

        var index = 0
        previewListViewItems.filter { o -> o.hasInsights }.forEach {
            methodsWithInsights.add(ListViewItem(it.name, index++))
        }
        return methodsWithInsights
    }

    fun hasInsights(): Boolean{
        return previewListViewItems.any { o -> o.hasInsights }
    }

    fun hasDiscoverableCodeObjects(): Boolean{
        return previewListViewItems.any{ o -> o.hasRelatedCodeObjects}
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


}




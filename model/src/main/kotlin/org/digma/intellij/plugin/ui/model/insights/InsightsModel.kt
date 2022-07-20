package org.digma.intellij.plugin.ui.model.insights

import org.digma.intellij.plugin.ui.model.*
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.*

class InsightsModel(private val envsSupplier: EnvironmentsSupplier) : PanelModel {

    var insightsCount: Int = 0
    var listViewItems: List<ListViewItem<*>> = Collections.emptyList()
    var previewListViewItems: List<ListViewItem<String>> = Collections.emptyList()
    var card: InsightsTabCard = InsightsTabCard.INSIGHTS
    var scope: Scope = EmptyScope("")


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

    override fun getEnvironmentsSupplier(): EnvironmentsSupplier {
        return envsSupplier
    }

    fun getPreviewListMessage(): String {
        if (scope is EmptyScope) {
            return "No code objects found for this document"
        } else if (previewListViewItems.isEmpty()) {
            return "No insights found for this document"
        } else {
            return "Try to click one of the following code objects"
        }
    }
}


enum class InsightsTabCard{
    INSIGHTS,PREVIEW
}

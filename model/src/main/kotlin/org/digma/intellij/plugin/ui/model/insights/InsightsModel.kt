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
import java.util.concurrent.atomic.AtomicReference

class InsightsModel : PanelModel {

    var insightsCount: Int = 0
    var listViewItems: List<ListViewItem<*>> = Collections.emptyList()
    var previewListViewItems: List<ListViewItem<String>> = Collections.emptyList()
    var usageStatusResultRef: AtomicReference<UsageStatusResult> = AtomicReference(EmptyUsageStatusResult)
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

    override fun getUsageStatus(): UsageStatusResult {
        return usageStatusResultRef.get()
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


enum class InsightsTabCard {
    INSIGHTS, PREVIEW
}

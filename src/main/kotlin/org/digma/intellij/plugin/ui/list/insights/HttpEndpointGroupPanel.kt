package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.model.rest.insights.*
import org.digma.intellij.plugin.ui.common.panelOfUnsupported
import org.digma.intellij.plugin.ui.model.insights.InsightGroupListViewItem
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.*
import javax.swing.JPanel

fun httpEndpointGroupPanel(listViewItem: InsightGroupListViewItem): JPanel {

    val items: SortedSet<ListViewItem<*>> = listViewItem.modelObject

    val result = panel {
        row("HTTP: ${listViewItem.groupId}") {}

        items.forEach {
            val modelObject = it.modelObject
            val cellItem =
                when (modelObject) {
                    is LowUsageInsight -> lowUsageInsightPanel(modelObject)
                    is NormalUsageInsight -> normalUsageInsightPanel(modelObject)
                    is HighUsageInsight -> highUsageInsightPanel(modelObject)
                    is SlowEndpointInsight -> slowEndpointPanel(modelObject)
                    is SlowestSpansInsight -> slowestSpansPanel(modelObject)
                    else -> panelOfUnsupported("${modelObject?.javaClass?.simpleName}")
                }

            row {
                cell(cellItem)
            }
        }
    }

    result.isOpaque = true
    return result
}

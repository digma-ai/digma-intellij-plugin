package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.*
import org.digma.intellij.plugin.ui.common.panelOfUnsupported
import org.digma.intellij.plugin.ui.model.insights.InsightGroupListViewItem
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.*
import javax.swing.JPanel

fun httpEndpointGroupPanel(listViewItem: InsightGroupListViewItem): JPanel {

    val items: SortedSet<ListViewItem<*>> = listViewItem.modelObject

    val result = panel {
        row {
            icon(Icons.INTERFACE_16).label("REST:", LabelPosition.LEFT)
            label("HTTP ").bold()
            label("${listViewItem.groupId}")
        }.topGap(TopGap.SMALL)


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
                cell(insightItemPanel(cellItem))
                    .horizontalAlign(HorizontalAlign.FILL)
            }.topGap(TopGap.MEDIUM)
        }
    }

    return insightGroupPanel(result)
}

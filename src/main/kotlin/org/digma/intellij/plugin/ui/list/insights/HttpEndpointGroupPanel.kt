package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.*
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.panelOfUnsupported
import org.digma.intellij.plugin.ui.model.insights.InsightGroupListViewItem
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.*
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

fun httpEndpointGroupPanel(listViewItem: InsightGroupListViewItem): JPanel {

    val items: SortedSet<ListViewItem<*>> = listViewItem.modelObject

    val result = panel {
        row("REST:") {
            cell(JLabel(headerAsHtml(listViewItem), Icons.Insight.HTTP_GROUP_TITLE, SwingConstants.LEFT))
                .bold()
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
                    else -> insightItemPanel(panelOfUnsupported("${modelObject?.javaClass?.simpleName}"))
                }

            row {
                cell(cellItem)
                    .horizontalAlign(HorizontalAlign.FILL)
            }.bottomGap(BottomGap.SMALL)
        }
    }

    return insightGroupPanel(result)
}

private fun headerAsHtml(listViewItem: InsightGroupListViewItem): String {
    // groupId contains "[get|post] [uri]"
    val split = listViewItem.groupId.split(' ');
    val httpMethod = split[0].uppercase()
    val httpRoute = split[1]
    return asHtml("HTTP $httpMethod $httpRoute")
}

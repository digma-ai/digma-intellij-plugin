package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.*
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.htmlSpanTitle
import org.digma.intellij.plugin.ui.common.panelOfUnsupported
import org.digma.intellij.plugin.ui.model.insights.InsightGroupListViewItem
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.*
import javax.swing.JPanel

fun httpEndpointGroupPanel(project: Project, listViewItem: InsightGroupListViewItem): JPanel {



    val result = panel {
        row("REST:") {
            val text = headerAsHtml(listViewItem)
            icon(Icons.Insight.HTTP_GROUP_TITLE).applyToComponent {
                toolTipText = text
            }.horizontalAlign(HorizontalAlign.LEFT).gap(RightGap.SMALL)
            label(text)
                .bold().applyToComponent {
                    toolTipText = text
                }.horizontalAlign(HorizontalAlign.LEFT)

        }.topGap(TopGap.SMALL)

        val items: SortedSet<ListViewItem<*>> = listViewItem.modelObject
        items.forEach {
            val modelObject = it.modelObject
            val cellItem =
                when (modelObject) {
                    is LowUsageInsight -> lowUsageInsightPanel(modelObject)
                    is NormalUsageInsight -> normalUsageInsightPanel(modelObject)
                    is HighUsageInsight -> highUsageInsightPanel(modelObject)
                    is SlowEndpointInsight -> slowEndpointPanel(modelObject)
                    is SlowestSpansInsight -> slowestSpansPanel(project,modelObject,it.moreData)
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
    val shortRouteName = EndpointSchema.getShortRouteName(listViewItem.groupId)
    // groupId contains "[get|post] [uri]"
    val split = shortRouteName.split(' ')
    val httpMethod = split[0].uppercase()
    val httpRoute = split[1]
    return asHtml("${htmlSpanTitle()}<b>HTTP $httpMethod $httpRoute</b>")
}

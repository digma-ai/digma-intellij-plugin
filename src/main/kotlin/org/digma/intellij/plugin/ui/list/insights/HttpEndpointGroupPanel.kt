package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.*
import org.digma.intellij.plugin.ui.common.*
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.model.insights.InsightGroupListViewItem
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.*
import javax.swing.JPanel

fun httpEndpointGroupPanel(project: Project, listViewItem: InsightGroupListViewItem,panelsLayoutHelper: PanelsLayoutHelper): JPanel {



    val result = panel {
        row(asHtml(spanGrayed("REST: "))) {
            val text = headerAsHtml(listViewItem)
            icon(Icons.Insight.HTTP_GROUP_TITLE).applyToComponent {
                toolTipText = text
            }.horizontalAlign(HorizontalAlign.LEFT).gap(RightGap.SMALL)
            cell(CopyableLabelHtml(text))
                .horizontalAlign(HorizontalAlign.LEFT)

        }.topGap(TopGap.SMALL)

        val items: SortedSet<ListViewItem<*>> = listViewItem.modelObject
        items.forEach {
            val modelObject = it.modelObject
            val cellItem =
                when (modelObject) {
                    is LowUsageInsight -> lowUsageInsightPanel(modelObject,panelsLayoutHelper)
                    is NormalUsageInsight -> normalUsageInsightPanel(modelObject,panelsLayoutHelper)
                    is HighUsageInsight -> highUsageInsightPanel(modelObject,panelsLayoutHelper)
                    is SlowEndpointInsight -> slowEndpointPanel(modelObject,panelsLayoutHelper)
                    is SlowestSpansInsight -> slowestSpansPanel(project,modelObject,it.moreData,panelsLayoutHelper)
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
    return asHtml("${spanBold("HTTP")} ${span("$httpMethod $httpRoute")}")
}

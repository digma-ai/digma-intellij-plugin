package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.model.rest.insights.*
import org.digma.intellij.plugin.ui.common.*
import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.model.insights.InsightGroupType.HttpEndpoint
import org.digma.intellij.plugin.ui.model.insights.InsightGroupType.Span
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import javax.swing.Icon
import javax.swing.JPanel


class InsightsListCellRenderer : AbstractPanelListCellRenderer() {


    override fun createPanel(project: Project,
                             value: ListViewItem<*>,
                             index: Int,
                             panelsLayoutHelper: PanelsLayoutHelper): JPanel {
        return getOrCreatePanel(project, value, panelsLayoutHelper)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getOrCreatePanel(project: Project,
                                 value: ListViewItem<*>,
                                 panelsLayoutHelper: PanelsLayoutHelper): JPanel {

        val panel = when (value.modelObject) {
            is InsightsList.GroupTitleModel -> buildGroupTitle(value.modelObject as InsightsList.GroupTitleModel)
            is HotspotInsight -> hotspotPanel(value.modelObject as HotspotInsight, panelsLayoutHelper)
            is ErrorInsight -> errorsPanel(project, value.modelObject as ErrorInsight, panelsLayoutHelper)
            is LowUsageInsight -> lowUsageInsightPanel(value.modelObject as LowUsageInsight, panelsLayoutHelper)
            is NormalUsageInsight -> normalUsageInsightPanel(value.modelObject as NormalUsageInsight,
                panelsLayoutHelper)
            is HighUsageInsight -> highUsageInsightPanel(value.modelObject as HighUsageInsight,
                panelsLayoutHelper)
            is SlowEndpointInsight -> slowEndpointPanel(value.modelObject as SlowEndpointInsight,
                panelsLayoutHelper)
            is SlowestSpansInsight -> slowestSpansPanel(project,
                value.modelObject as SlowestSpansInsight, value.moreData, panelsLayoutHelper)
            is SpanInsight -> spanPanel(value.modelObject as SpanInsight)
            is SpanDurationsInsight -> spanDurationPanel(value.modelObject as SpanDurationsInsight,
                panelsLayoutHelper)
            is UnmappedInsight -> unmappedInsightPanel(value.modelObject as UnmappedInsight,
                panelsLayoutHelper)
            else -> genericPanelForSingleInsight(value.modelObject, panelsLayoutHelper)
        }

        return panel
    }


    private fun buildGroupTitle(value: InsightsList.GroupTitleModel): JPanel {

        val panel = when (value.type) {
            HttpEndpoint -> httpEndpointGroupTitle(value)
            Span -> spanGroupTitle(value)
            else -> defaultInsightGroupTitle(value)
        }

        return insightTitlePanel(panel)

    }


    private fun defaultInsightGroupTitle(value: InsightsList.GroupTitleModel): JPanel {
        return groupTitlePanel("Unknown: ", value.groupId, Laf.Icons.Insight.TELESCOPE)
    }


    private fun spanGroupTitle(value: InsightsList.GroupTitleModel): JPanel {
        return groupTitlePanel("Span: ", value.groupId, Laf.Icons.Insight.TELESCOPE)
    }

    private fun httpEndpointGroupTitle(value: InsightsList.GroupTitleModel): JPanel {
        val labelText = headerAsHtml(value)
        return groupTitlePanel("REST: ", labelText, Laf.Icons.Insight.INTERFACE)
    }



    private fun groupTitlePanel(titleText: String, labelText: String,icon: Icon): JPanel {
        return panel {
            row(asHtml(spanGrayed(titleText))) {
                icon(icon).applyToComponent {
                    toolTipText = labelText
                }.horizontalAlign(HorizontalAlign.LEFT).gap(RightGap.SMALL)
                cell(CopyableLabelHtml(labelText)).applyToComponent {
                    toolTipText = labelText
                }.horizontalAlign(HorizontalAlign.LEFT)
            }
        }
    }





    private fun headerAsHtml(value: InsightsList.GroupTitleModel): String {
        val shortRouteName = EndpointSchema.getShortRouteName(value.groupId)
        // groupId contains "[get|post] [uri]"
        val split = shortRouteName.split(' ')
        val httpMethod = split[0].uppercase()
        val httpRoute = split[1]
        return asHtml("${spanBold("HTTP")} ${span("$httpMethod $httpRoute")}")
    }
}
package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight
import org.digma.intellij.plugin.model.rest.insights.HighUsageInsight
import org.digma.intellij.plugin.model.rest.insights.HotspotInsight
import org.digma.intellij.plugin.model.rest.insights.LowUsageInsight
import org.digma.intellij.plugin.model.rest.insights.NormalUsageInsight
import org.digma.intellij.plugin.model.rest.insights.SlowEndpointInsight
import org.digma.intellij.plugin.model.rest.insights.SlowestSpansInsight
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsInsight
import org.digma.intellij.plugin.model.rest.insights.SpanInsight
import org.digma.intellij.plugin.model.rest.insights.UnmappedInsight
import org.digma.intellij.plugin.ui.common.CopyableLabelHtml
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.span
import org.digma.intellij.plugin.ui.common.spanBold
import org.digma.intellij.plugin.ui.common.spanGrayed
import org.digma.intellij.plugin.model.rest.insights.*
import org.digma.intellij.plugin.model.rest.insights.EndpointSchema.Companion.CONSUMER_SCHEMA
import org.digma.intellij.plugin.model.rest.insights.EndpointSchema.Companion.HTTP_SCHEMA
import org.digma.intellij.plugin.model.rest.insights.EndpointSchema.Companion.RPC_SCHEMA
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
            is SpanDurationsInsight -> spanDurationPanel(project, value.modelObject as SpanDurationsInsight,
                panelsLayoutHelper)
            is UnmappedInsight -> unmappedInsightPanel(value.modelObject as UnmappedInsight,
                panelsLayoutHelper)
            else -> genericPanelForSingleInsight(value.modelObject, panelsLayoutHelper)
        }

        return panel
    }


    private fun buildGroupTitle(value: InsightsList.GroupTitleModel): JPanel {

        val panel = when (value.type) {
            HttpEndpoint -> endpointGroupTitle(value)
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

    private fun endpointGroupTitle(value: InsightsList.GroupTitleModel): JPanel {
        val groupViewModel = createEndpointGroupViewModel(value.groupId);
        return groupTitlePanel(groupViewModel.titleText, groupViewModel.labelText,groupViewModel.icon)
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

    private fun createEndpointGroupViewModel(fullRouteName: String): GroupViewModel{
        val routeInfo = EndpointSchema.getRouteInfo(fullRouteName);
        val endpoint =  routeInfo.shortName
        if(routeInfo.schema == HTTP_SCHEMA){
            val split =endpoint.split(' ')
            return GroupViewModel("REST: ", asHtml("${spanBold("HTTP")} ${span("${split[0].uppercase()} ${split[1]}")}"),  Laf.Icons.Insight.INTERFACE)
        }
        if(routeInfo.schema == RPC_SCHEMA){
            return GroupViewModel("RPC: ", asHtml(span(endpoint)),  Laf.Icons.Insight.INTERFACE)
        }
        if(routeInfo.schema == CONSUMER_SCHEMA){
            return GroupViewModel("CONSUMER: ", asHtml(span(endpoint)),  Laf.Icons.Insight.MESSAGE)
        }
        return GroupViewModel("REST: ", asHtml(""),  Laf.Icons.Insight.INTERFACE)
    }
}
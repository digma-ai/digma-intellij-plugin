package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.model.rest.insights.EPNPlusSpansInsight
import org.digma.intellij.plugin.model.rest.insights.EndpointSchema
import org.digma.intellij.plugin.model.rest.insights.EndpointSchema.Companion.CONSUMER_SCHEMA
import org.digma.intellij.plugin.model.rest.insights.EndpointSchema.Companion.HTTP_SCHEMA
import org.digma.intellij.plugin.model.rest.insights.EndpointSchema.Companion.RPC_SCHEMA
import org.digma.intellij.plugin.model.rest.insights.EndpointSchema.Companion.SPAN_SCHEMA
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight
import org.digma.intellij.plugin.model.rest.insights.GroupViewModel
import org.digma.intellij.plugin.model.rest.insights.HighUsageInsight
import org.digma.intellij.plugin.model.rest.insights.HotspotInsight
import org.digma.intellij.plugin.model.rest.insights.LowUsageInsight
import org.digma.intellij.plugin.model.rest.insights.NormalUsageInsight
import org.digma.intellij.plugin.model.rest.insights.SlowEndpointInsight
import org.digma.intellij.plugin.model.rest.insights.SlowestSpansInsight
import org.digma.intellij.plugin.model.rest.insights.SpanDurationBreakdownInsight
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsInsight
import org.digma.intellij.plugin.model.rest.insights.SpanNPlusOneInsight
import org.digma.intellij.plugin.model.rest.insights.SpanScalingInsight
import org.digma.intellij.plugin.model.rest.insights.SpanScalingRootCauseInsight
import org.digma.intellij.plugin.model.rest.insights.SpanSlowEndpointsInsight
import org.digma.intellij.plugin.model.rest.insights.SpanUsagesInsight
import org.digma.intellij.plugin.model.rest.insights.UnmappedInsight
import org.digma.intellij.plugin.ui.common.CopyableLabelHtml
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.span
import org.digma.intellij.plugin.ui.common.spanBold
import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.model.insights.InsightGroupType.HttpEndpoint
import org.digma.intellij.plugin.ui.model.insights.InsightGroupType.Span
import org.digma.intellij.plugin.ui.model.insights.NoDataYet
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

    private fun getOrCreatePanel(project: Project,
                                 value: ListViewItem<*>,
                                 panelsLayoutHelper: PanelsLayoutHelper): JPanel {

        val panel = when (value.modelObject) {
            is InsightsList.GroupTitleModel -> buildGroupTitle(value.modelObject as InsightsList.GroupTitleModel)
            is HotspotInsight -> hotspotPanel(project, value.modelObject as HotspotInsight)
            is ErrorInsight -> errorsPanel(project, value.modelObject as ErrorInsight)
            is LowUsageInsight -> lowUsageInsightPanel(project, value.modelObject as LowUsageInsight)
            is NormalUsageInsight -> normalUsageInsightPanel(project, value.modelObject as NormalUsageInsight)
            is HighUsageInsight -> highUsageInsightPanel(project, value.modelObject as HighUsageInsight)
            is EPNPlusSpansInsight -> ePNPlusSpansPanel(project, value.modelObject as EPNPlusSpansInsight, value.moreData)
            is SpanNPlusOneInsight -> spanNPlusOneInsightPanel(project, value.modelObject as SpanNPlusOneInsight)
            is SlowEndpointInsight -> slowEndpointPanel(project, value.modelObject as SlowEndpointInsight)
            is SlowestSpansInsight -> slowestSpansPanel(project, value.modelObject as SlowestSpansInsight, value.moreData)
            is SpanUsagesInsight -> spanUsagesPanel(project, value.modelObject as SpanUsagesInsight, value.moreData)
            is SpanDurationsInsight -> spanDurationPanel(project, value.modelObject as SpanDurationsInsight, panelsLayoutHelper)
            is SpanDurationBreakdownInsight -> spanDurationBreakdownPanel(project, value.modelObject as SpanDurationBreakdownInsight, value.moreData)
            is SpanSlowEndpointsInsight -> spanSlowEndpointsPanel(project, value.modelObject as SpanSlowEndpointsInsight, value.moreData)
            is SpanScalingInsight -> spanScalingListViewItemsPanel(project, value.modelObject as SpanScalingInsight, value.moreData)
            is SpanScalingRootCauseInsight -> spanScalingRootCauseItemsPanel(project, value.modelObject as SpanScalingRootCauseInsight, value.moreData)
            is UnmappedInsight -> unmappedInsightPanel(project, value.modelObject as UnmappedInsight)
            is NoDataYet -> noDataYetInsightPanel()
            else -> genericPanelForSingleInsight(project, value.modelObject)
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
        return groupTitlePanel(value.groupId, Laf.Icons.Insight.TELESCOPE)
    }


    private fun spanGroupTitle(value: InsightsList.GroupTitleModel): JPanel {
        return groupTitlePanel(value.groupId, Laf.Icons.Insight.TELESCOPE)
    }

    private fun endpointGroupTitle(value: InsightsList.GroupTitleModel): JPanel {
        val groupViewModel = createEndpointGroupViewModel(value.route)
        return groupTitlePanel(groupViewModel.labelText, groupViewModel.icon)
    }


    private fun groupTitlePanel(labelText: String, icon: Icon): JPanel {
        return panel {
            row {
                icon(icon).applyToComponent {
                    toolTipText = labelText
                }.horizontalAlign(HorizontalAlign.LEFT).gap(RightGap.SMALL)
                cell(CopyableLabelHtml(labelText)).applyToComponent {
                    toolTipText = labelText
                }.horizontalAlign(HorizontalAlign.LEFT)
            }
        }
    }

    private fun createEndpointGroupViewModel(fullRouteName: String): GroupViewModel {
        val routeInfo = EndpointSchema.getRouteInfo(fullRouteName)
        val endpoint = routeInfo.shortName
        if (routeInfo.schema == HTTP_SCHEMA) {
            val split = endpoint.split(' ')
            val lastElementIndex = split.size - 1
            val schemaName = if (split.size < 3) {
                "HTTP"
            } else {
                split[lastElementIndex - 2].uppercase()
            }
            return GroupViewModel(asHtml("${spanBold(schemaName)} ${span("${split[lastElementIndex - 1].uppercase()} ${split[lastElementIndex]}")}"), Laf.Icons.Insight.INTERFACE)
        }
        if (routeInfo.schema == RPC_SCHEMA) {
            return GroupViewModel(asHtml(span(endpoint)), Laf.Icons.Insight.INTERFACE)
        }
        if (routeInfo.schema == CONSUMER_SCHEMA) {
            return GroupViewModel(asHtml(span(endpoint)), Laf.Icons.Insight.MESSAGE)
        }
        if (routeInfo.schema == SPAN_SCHEMA) {
            return GroupViewModel(asHtml(span(endpoint)), Laf.Icons.Insight.TELESCOPE)
        }
        return GroupViewModel(asHtml(""), Laf.Icons.Insight.INTERFACE)
    }

    private fun unmappedInsightPanel(project: Project, modelObject: UnmappedInsight): JPanel {

        val methodName = modelObject.codeObjectId.substringAfterLast("\$_\$")
        return createInsightPanel(
                project = project,
                insight = modelObject,
                title = "Unmapped insight: '${modelObject.theType}'",
                description = "unmapped insight type for '$methodName'",
                iconsList = listOf(Laf.Icons.Insight.QUESTION_MARK),
                bodyPanel = null,
                buttons = null,
                paginationComponent = null
        )
    }
}
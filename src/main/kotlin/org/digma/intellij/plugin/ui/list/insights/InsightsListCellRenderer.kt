package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight
import org.digma.intellij.plugin.model.rest.insights.HotspotInsight
import org.digma.intellij.plugin.model.rest.insights.UnmappedInsight
import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.model.insights.InsightGroupListViewItem
import org.digma.intellij.plugin.ui.model.insights.InsightGroupType.HttpEndpoint
import org.digma.intellij.plugin.ui.model.insights.InsightGroupType.Span
import org.digma.intellij.plugin.ui.model.listview.GroupListViewItem
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import javax.swing.JPanel


class InsightsListCellRenderer : AbstractPanelListCellRenderer() {


    override fun createPanel(project: Project,
                             value: ListViewItem<*>,
                             index: Int,
                             panelsLayoutHelper: PanelsLayoutHelper): JPanel {
        return getOrCreatePanel(project, index, value,panelsLayoutHelper)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getOrCreatePanel(project: Project,
                                 index: Int,
                                 value: ListViewItem<*>,
                                 panelsLayoutHelper: PanelsLayoutHelper): JPanel {

        val panel = when (value) {
            is GroupListViewItem -> buildGroupPanel(project,value,panelsLayoutHelper)
            else -> {
                when (value.modelObject) {
                    is HotspotInsight -> hotspotPanel(value as ListViewItem<HotspotInsight>,panelsLayoutHelper)
                    is ErrorInsight -> errorsPanel(project, value as ListViewItem<ErrorInsight>,panelsLayoutHelper)
                    is UnmappedInsight -> unmappedInsightPanel(value as ListViewItem<UnmappedInsight>,panelsLayoutHelper)
                    else -> genericPanelForSingleInsight(value as ListViewItem<CodeObjectInsight>,panelsLayoutHelper)
                }
            }
        }

        return panel
    }


    private fun buildGroupPanel(project: Project, value: GroupListViewItem, panelsLayoutHelper: PanelsLayoutHelper): JPanel {

        val panel =
            when (value) {
                is InsightGroupListViewItem -> {
                    when (value.type) {
                        HttpEndpoint -> httpEndpointGroupPanel(project,value,panelsLayoutHelper)
                        Span -> spanGroupPanel(value,panelsLayoutHelper)
                        else -> insightGroupPanel(panelOfUnsupported("group type: ${value.type}"))
                    }
                }
                else -> insightGroupPanel(panelOfUnsupported("group class: ${value.javaClass.simpleName}"))
            }
        return panel
    }




}
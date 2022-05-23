package org.digma.intellij.plugin.ui.list.insights

import org.digma.intellij.plugin.model.rest.insights.ErrorInsight
import org.digma.intellij.plugin.model.rest.insights.HotspotInsight
import org.digma.intellij.plugin.ui.common.emptyPanel
import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.model.insights.InsightGroupListViewItem
import org.digma.intellij.plugin.ui.model.insights.InsightGroupType.HttpEndpoint
import org.digma.intellij.plugin.ui.model.insights.InsightGroupType.Span
import org.digma.intellij.plugin.ui.model.listview.GroupListViewItem
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.*
import javax.swing.JPanel

class InsightsListCellRenderer : AbstractPanelListCellRenderer() {


    override fun createPanel(value: ListViewItem<*>, index: Int): JPanel {
        return getOrCreatePanel(index, value)
    }


    @Suppress("UNCHECKED_CAST")
    private fun getOrCreatePanel(index: Int, value: ListViewItem<*>): JPanel {


        val panel = when (value.modelObject) {
            is HotspotInsight -> {
                hotspotPanel(value as ListViewItem<HotspotInsight>)
            }
            is TreeSet<*> -> {
                buildGroupPanel(value as GroupListViewItem)
            }
            is ErrorInsight -> {
                errorsPanel(value as ListViewItem<ErrorInsight>)
            }
            else -> {
                emptyPanel(value)
            }
        }


        return panel
    }

    private fun buildGroupPanel(value: GroupListViewItem): JPanel {

        when (value) {
            is InsightGroupListViewItem -> {
                when (value.type) {
                    HttpEndpoint -> {

                    }
                    Span -> {
                        return spanGroupPanel(value)
                    }
                    else -> {
                        return emptyPanel(value)
                    }
                }
            }
        }

        return emptyPanel(value)

    }



}
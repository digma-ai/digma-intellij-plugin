package org.digma.intellij.plugin.ui.list.insights

import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight
import org.digma.intellij.plugin.model.rest.insights.HotspotInsight
import org.digma.intellij.plugin.ui.common.emptyPanel
import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.model.insights.InsightGroupListViewItem
import org.digma.intellij.plugin.ui.model.insights.InsightGroupType.HttpEndpoint
import org.digma.intellij.plugin.ui.model.insights.InsightGroupType.Span
import org.digma.intellij.plugin.ui.model.listview.GroupListViewItem
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import javax.swing.JPanel
import javax.swing.event.ListDataEvent

class InsightsListCellRenderer : AbstractPanelListCellRenderer() {

    private val panels: MutableMap<Int, JPanel> = HashMap()


    override fun createPanel(value: ListViewItem<*>, index: Int): JPanel {
        return getOrCreatePanel(index, value)
    }


    @Suppress("UNCHECKED_CAST")
    private fun getOrCreatePanel(index: Int, value: ListViewItem<*>): JPanel {

        if (panels.containsKey(index)) {
            return panels[index]!!
        }

        val panel = when (value) {
            is GroupListViewItem -> buildGroupPanel(value)
            else -> {
                when (value.modelObject) {
                    is HotspotInsight -> hotspotPanel(value as ListViewItem<HotspotInsight>)
                    is ErrorInsight -> errorsPanel(value as ListViewItem<ErrorInsight>)
                    else -> genericPanelForSingleInsight(value as ListViewItem<CodeObjectInsight>)
                }
            }
        }

        panels[index] = panel

        return panel
    }


    private fun buildGroupPanel(value: GroupListViewItem): JPanel {

        val panel =
            when (value) {
                is InsightGroupListViewItem -> {
                    when (value.type) {
                        HttpEndpoint -> httpEndpointGroupPanel(value)
                        Span -> spanGroupPanel(value)
                        else -> panelOfUnsupported("group type: ${value.type}")
                    }
                }
                else -> panelOfUnsupported("group class: ${value.javaClass.simpleName}")
            }
        return panel
    }


    override fun intervalAdded(e: ListDataEvent?) {
        panels.clear()
    }

    override fun intervalRemoved(e: ListDataEvent?) {
        panels.clear()
    }

    override fun contentsChanged(e: ListDataEvent?) {
        panels.clear()
    }


}
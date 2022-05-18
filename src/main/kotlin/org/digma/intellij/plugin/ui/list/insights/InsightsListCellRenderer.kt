package org.digma.intellij.plugin.ui.list.insights

import org.digma.intellij.plugin.model.rest.insights.ErrorInsight
import org.digma.intellij.plugin.model.rest.insights.HotspotInsight
import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.model.listview.Group
import org.digma.intellij.plugin.ui.model.listview.GroupListViewItem
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.Component
import javax.swing.JPanel
import javax.swing.event.ListDataEvent

class InsightsListCellRenderer : AbstractPanelListCellRenderer<Any>() {

    val panels: MutableMap<Int, JPanel> = HashMap()


    override fun createPanel(value: ListViewItem<Any>, index: Int): Component {
        return getOrCreatePanel(index, value)
    }


    private fun getOrCreatePanel(index: Int, value: ListViewItem<Any>): JPanel {

        if (panels.containsKey(index)) {
            return panels[index]!!
        }

        val panel: JPanel
        when (value.getModel()) {
            is HotspotInsight -> panel = hotspotPanel(value as ListViewItem<HotspotInsight>)
            is Group -> panel = selectGroupPanel(value as GroupListViewItem)
            is ErrorInsight -> panel = errorsPanel(value as ListViewItem<ErrorInsight>)
            else -> {
                panel = emptyPanel(value)
            }
        }



        panels[index] = panel

        return panel
    }

    private fun selectGroupPanel(value: GroupListViewItem): JPanel {
        //todo: select
        return spanGroupPanel(value)
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
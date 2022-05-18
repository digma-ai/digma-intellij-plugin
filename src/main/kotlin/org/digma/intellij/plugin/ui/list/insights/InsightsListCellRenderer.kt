package org.digma.intellij.plugin.ui.list.insights

import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.model.insights.ErrorsListViewItem
import org.digma.intellij.plugin.ui.model.insights.HotspotListViewItem
import org.digma.intellij.plugin.ui.model.insights.SpanGroupListViewItem
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.Color
import java.awt.Component
import javax.swing.JPanel
import javax.swing.event.ListDataEvent

class InsightsListCellRenderer : AbstractPanelListCellRenderer<ListViewItem>() {

    val panels: MutableMap<Int, JPanel> = HashMap()


    override fun createPanel(value: ListViewItem, index: Int): Component {
        return getOrCreatePanel(index, value)
    }


    private fun getOrCreatePanel(index: Int, value: ListViewItem): JPanel {

        if (panels.containsKey(index)) {
            return panels[index]!!
        }

        val panel: JPanel
        when (value) {
            is HotspotListViewItem -> panel = hotspotPanel(value)
            is SpanGroupListViewItem -> panel = spanGroupPanel(value)
            is ErrorsListViewItem -> panel = errorsPanel(value)
            else -> {
                panel = emptyPanel(value)
            }
        }

        panels[index] = panel

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
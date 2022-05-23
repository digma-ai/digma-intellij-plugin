package org.digma.intellij.plugin.ui.list.insights

import org.digma.intellij.plugin.ui.list.PanelList
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.Color

class InsightsList(listViewItems: List<ListViewItem<*>>) : PanelList(listViewItems){

    init {
        this.setCellRenderer(InsightsListCellRenderer())
    }

    override fun getListBackground(): Color {
        return insightListBackground()
    }
}
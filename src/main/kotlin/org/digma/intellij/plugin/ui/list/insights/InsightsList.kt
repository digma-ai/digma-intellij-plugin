package org.digma.intellij.plugin.ui.list.insights

import org.digma.intellij.plugin.ui.list.PanelList
import org.digma.intellij.plugin.ui.model.listview.ListViewItem

class InsightsList(listViewItems: List<ListViewItem<*>>) : PanelList(listViewItems){

    init {
        this.setCellRenderer(InsightsListCellRenderer())
    }
}
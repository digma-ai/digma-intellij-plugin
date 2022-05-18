package org.digma.intellij.plugin.ui.list.insights

import org.digma.intellij.plugin.ui.list.PanelList
import org.digma.intellij.plugin.ui.model.insights.InsightListViewItem
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import javax.swing.JPanel

class InsightsList : PanelList<InsightListViewItem>(){

    init {
        this.setCellRenderer(InsightsListCellRenderer())
    }
}
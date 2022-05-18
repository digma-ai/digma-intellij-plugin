package org.digma.intellij.plugin.ui.list.insights

import org.digma.intellij.plugin.ui.list.PanelList

class InsightsList : PanelList<Any>(){

    init {
        this.setCellRenderer(InsightsListCellRenderer())
    }
}
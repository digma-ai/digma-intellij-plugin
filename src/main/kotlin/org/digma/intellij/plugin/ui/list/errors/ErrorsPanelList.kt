package org.digma.intellij.plugin.ui.list.errors

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.list.PanelList
import org.digma.intellij.plugin.ui.list.insights.insightListBackground
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.Color

class ErrorsPanelList(project: Project, listViewItems: List<ListViewItem<*>>) : PanelList(project, listViewItems) {

    init {
        this.setCellRenderer(ErrorsPanelListCellRenderer())
    }

    override fun getListBackground(): Color {
        return insightListBackground()
    }
}
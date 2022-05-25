package org.digma.intellij.plugin.ui.list.errors

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.list.PanelList
import org.digma.intellij.plugin.ui.list.insights.panelsListBackground
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.Color

class ErrorsList(project: Project, listViewItems: List<ListViewItem<*>>) : PanelList(project,listViewItems){

    init {
        this.setCellRenderer(ErrorsListCellRenderer())
    }

    override fun getListBackground(): Color {
        return panelsListBackground()
    }
}
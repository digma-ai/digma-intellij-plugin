package org.digma.intellij.plugin.ui.list.errors

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.list.PanelList
import org.digma.intellij.plugin.ui.model.listview.ListViewItem

class ErrorsPanelList(project: Project, listViewItems: List<ListViewItem<*>>) : PanelList(project, DefaultPanelListModel(listViewItems)) {

    init {
        this.setCellRenderer(ErrorsPanelListCellRenderer())
    }

}
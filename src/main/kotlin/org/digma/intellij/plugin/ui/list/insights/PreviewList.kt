package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.list.PanelList
import org.digma.intellij.plugin.ui.model.listview.ListViewItem

class PreviewList(project: Project, listViewItems: List<ListViewItem<*>>) : PanelList(project,DefaultPanelListModel(listViewItems)){

    init {
        this.setCellRenderer(PreviewListCellRenderer())
    }

}
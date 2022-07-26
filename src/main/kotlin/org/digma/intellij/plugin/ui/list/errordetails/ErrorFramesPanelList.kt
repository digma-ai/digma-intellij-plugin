package org.digma.intellij.plugin.ui.list.errordetails

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.list.PanelList
import org.digma.intellij.plugin.ui.model.listview.ListViewItem

class ErrorFramesPanelList(project: Project, listViewItems: List<ListViewItem<*>>,gapBetweenItems:Boolean) : PanelList(project, DefaultPanelListModel(listViewItems),gapBetweenItems) {

    init {
        this.setCellRenderer(ErrorFramesPanelListCellRenderer())
    }

}
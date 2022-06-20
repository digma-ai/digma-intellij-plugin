package org.digma.intellij.plugin.ui.list.errordetails

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.list.PanelList
import org.digma.intellij.plugin.ui.list.panelListBackground
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.Color

class ErrorFramesPanelList(project: Project, listViewItems: List<ListViewItem<*>>,gapBetweenItems:Boolean) : PanelList(project, listViewItems,gapBetweenItems) {

    init {
        this.setCellRenderer(ErrorFramesPanelListCellRenderer())
    }

    override fun getListBackground(): Color {
        return panelListBackground()
    }
}
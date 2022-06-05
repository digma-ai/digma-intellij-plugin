package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.list.PanelList
import org.digma.intellij.plugin.ui.list.panelListBackground
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.Color

class PreviewList(project: Project, listViewItems: List<ListViewItem<*>>) : PanelList(project,listViewItems){

    init {
        this.setCellRenderer(PreviewListCellRenderer())
    }

    override fun getListBackground(): Color {
        return panelListBackground()
    }
}
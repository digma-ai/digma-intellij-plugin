package org.digma.intellij.plugin.ui.list

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import javax.swing.JPanel
import javax.swing.event.ListDataListener

interface PanelListCellRenderer: ListDataListener {

    fun getListCellRendererComponent(project: Project,
                                     list: PanelList,
                                     value: ListViewItem<*>,
                                     index: Int,
                                     cellHasFocus: Boolean,
                                     panelsLayoutHelper: PanelsLayoutHelper): JPanel

}
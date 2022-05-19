package org.digma.intellij.plugin.ui.list

import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import javax.swing.JPanel
import javax.swing.event.ListDataListener

interface PanelListCellRenderer: ListDataListener {

    fun getListCellRendererComponent(list: PanelList,
                                              value: ListViewItem<*>,
                                              index: Int,
                                              cellHasFocus: Boolean): JPanel


}
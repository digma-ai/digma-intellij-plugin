package org.digma.intellij.plugin.ui.list

import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.Component
import javax.swing.event.ListDataListener

interface PanelListCellRenderer<T: ListViewItem>: ListDataListener {

    fun getListCellRendererComponent(list: PanelList<out ListViewItem>,
                                              value: T,
                                              index: Int,
                                              cellHasFocus: Boolean): Component


}
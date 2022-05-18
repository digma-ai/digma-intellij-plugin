package org.digma.intellij.plugin.ui.list

import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.Component
import javax.swing.event.ListDataListener

interface PanelListCellRenderer<T>: ListDataListener {

    fun getListCellRendererComponent(list: PanelList<T>,
                                              value: ListViewItem<T>,
                                              index: Int,
                                              cellHasFocus: Boolean): Component


}
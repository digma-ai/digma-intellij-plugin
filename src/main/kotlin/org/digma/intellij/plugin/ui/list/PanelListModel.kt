package org.digma.intellij.plugin.ui.list

import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import javax.swing.ListModel

interface PanelListModel<T> : ListModel<ListViewItem<T>> {

    //todo: change to T when all interfaces are ready
    fun setListData(listViewItems: List<ListViewItem<T>>)

}
package org.digma.intellij.plugin.ui.list

import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import javax.swing.ListModel

interface PanelListModel : ListModel<ListViewItem<*>> {

    fun setListData(listViewItems: List<ListViewItem<*>>)

}
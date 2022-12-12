package org.digma.intellij.plugin.ui.model.listview

import java.util.*

open class GroupListViewItem(sortIndex: Int, val groupId: String, val route: String) : ListViewItem<SortedSet<ListViewItem<*>>>(
    TreeSet(Comparator.comparingInt(ListViewItem<*>::sortIndex)),
    sortIndex
) {

    fun addItem(item: ListViewItem<*>) {
        modelObject.add(item)
    }
}
package org.digma.intellij.plugin.ui.model.listview

import java.util.*

abstract class GroupListViewItem(sortIndex: Int, groupId: String) : ListViewItem<SortedSet<ListViewItem<Any>>>(
    TreeSet(Comparator.comparingInt(ListViewItem<Any>::sortIndex)),
    sortIndex,
    groupId
) {

    fun addItem(item: ListViewItem<Any>) {
        modelObject.add(item)
    }
}
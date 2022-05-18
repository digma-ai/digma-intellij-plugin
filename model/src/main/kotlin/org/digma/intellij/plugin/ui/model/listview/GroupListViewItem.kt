package org.digma.intellij.plugin.ui.model.listview

import java.util.*

open class GroupListViewItem(sortIndex: Int, val groupId: String) : ListViewItem<SortedSet<ListViewItem<Any>>>(
    TreeSet(Comparator.comparingInt(ListViewItem<Any>::sortIndex)),
    sortIndex
) {

    fun addItem(item: ListViewItem<Any>) {
        modelObject.add(item)
    }
}
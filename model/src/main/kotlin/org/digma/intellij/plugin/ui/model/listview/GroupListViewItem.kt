package org.digma.intellij.plugin.ui.model.listview

import java.util.*

abstract class GroupListViewItem(sortIndex: Int, groupId: String) : ListViewItem<Any>(Object(), sortIndex, groupId) {

    private val items: SortedSet<ListViewItem<Any>> = TreeSet(Comparator.comparingInt(ListViewItem<Any>::sortIndex))

    fun addItem(item: ListViewItem<Any>) {
        items.add(item)
    }
}
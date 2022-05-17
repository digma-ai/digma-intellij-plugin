package org.digma.intellij.plugin.ui.model.listview

import java.util.*

abstract class GroupListViewItem(sortIndex: Int, groupId: String) : ListViewItem(sortIndex, groupId) {

    private val items: SortedSet<ListViewItem> = TreeSet(Comparator.comparingInt(ListViewItem::sortIndex))

    fun addItem(item: ListViewItem) {
        items.add(item)
    }
}
package org.digma.intellij.plugin.ui.model.listview

import java.util.*

abstract class GroupListViewItem(val groupId: String, sortIndex: Int) : ListViewItem(sortIndex) {

    val items: SortedSet<ListViewItem> = TreeSet(Comparator.comparingInt(ListViewItem::sortIndex))


}
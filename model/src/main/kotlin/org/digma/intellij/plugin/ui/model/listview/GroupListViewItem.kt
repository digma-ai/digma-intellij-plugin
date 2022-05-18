package org.digma.intellij.plugin.ui.model.listview

import org.digma.intellij.plugin.ui.common.trimLastChar
import java.util.*

open class GroupListViewItem(sortIndex: Int, val groupId: String) : ListViewItem<SortedSet<ListViewItem<*>>>(
    TreeSet(Comparator.comparingInt(ListViewItem<*>::sortIndex)),
    sortIndex
) {

    override fun toString(): String {
        return "${super.toString().trimLastChar()}, groupId=$groupId)"
    }

    fun addItem(item: ListViewItem<*>) {
        modelObject.add(item)
    }
}
package org.digma.intellij.plugin.ui.model.listview

abstract class GroupListViewItem(val groupId: String) : ListViewItem() {

    val items: MutableList<ListViewItem> = ArrayList()

}
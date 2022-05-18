package org.digma.intellij.plugin.ui.list

import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.*
import javax.swing.AbstractListModel
import kotlin.collections.ArrayList

abstract class AbstractPanelListModel<E : ListViewItem> : AbstractListModel<ListViewItem>(),
    PanelListModel<ListViewItem> {

    private var items: List<ListViewItem> = ArrayList()

    override fun setListData(listViewItems: List<ListViewItem>) {
        this.items = listViewItems

        //temp: duplicate
//        var newItems = ArrayList(listViewItems)
//        newItems.addAll(listViewItems)
//        newItems.addAll(listViewItems)
//        this.items = newItems
        //env temp


        Collections.sort(this.items, Comparator.comparingInt(ListViewItem::sortIndex))
        fireContentsChanged(this, 0, items.size - 1)
    }


    override fun getSize(): Int {
        return items.size
    }

    override fun getElementAt(index: Int): ListViewItem {
        if (index < 0 || index >= items.size) {
            throw java.lang.IllegalArgumentException("index out of bounds $index")
        }
        return items[index]
    }

}
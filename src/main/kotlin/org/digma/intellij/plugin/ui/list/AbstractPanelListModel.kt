package org.digma.intellij.plugin.ui.list

import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.*
import javax.swing.AbstractListModel

abstract class AbstractPanelListModel<T> : AbstractListModel<ListViewItem<T>>(),
    PanelListModel<T> {

    private var items: List<ListViewItem<T>> = ArrayList()

    override fun setListData(listViewItems: List<ListViewItem<T>>) {
        this.items = listViewItems

        //temp: duplicate
//        var newItems = ArrayList(listViewItems)
//        newItems.addAll(listViewItems)
//        newItems.addAll(listViewItems)
//        this.items = newItems
        //env temp


        Collections.sort(this.items, Comparator.comparingInt(ListViewItem<T>::sortIndex))
        fireContentsChanged(this, 0, items.size - 1)
    }


    override fun getSize(): Int {
        return items.size
    }

    override fun getElementAt(index: Int): ListViewItem<T> {
        if (index < 0 || index >= items.size) {
            throw java.lang.IllegalArgumentException("index out of bounds $index")
        }
        return items[index]
    }

}
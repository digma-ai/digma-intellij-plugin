package org.digma.intellij.plugin.ui.list

import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.*
import javax.swing.AbstractListModel

abstract class AbstractPanelListModel : AbstractListModel<ListViewItem<*>>(),
    PanelListModel {

    private val LOGGER = Logger.getInstance(PanelList::class.java)

    private var items: List<ListViewItem<*>> = ArrayList()

    override fun setListData(listViewItems: List<ListViewItem<*>>) {
        this.items = listViewItems

        //temp: duplicate
//        var newItems = ArrayList(listViewItems)
//        newItems.addAll(listViewItems)
//        newItems.addAll(listViewItems)
//        this.items = newItems
        //env temp

        Log.log(LOGGER::info, "setListData {}", items)

        Collections.sort(this.items, Comparator.comparingInt(ListViewItem<*>::sortIndex))
        fireContentsChanged(this, 0, items.size - 1)
    }


    override fun getSize(): Int {
        return items.size
    }

    override fun getElementAt(index: Int): ListViewItem<*> {
        if (index < 0 || index >= items.size) {
            throw java.lang.IllegalArgumentException("index out of bounds $index")
        }
        return items[index]
    }

}
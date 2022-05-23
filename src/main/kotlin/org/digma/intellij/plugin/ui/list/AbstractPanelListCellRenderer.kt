package org.digma.intellij.plugin.ui.list

import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import javax.swing.JPanel
import javax.swing.event.ListDataEvent

abstract class AbstractPanelListCellRenderer: PanelListCellRenderer {

    private val panels: MutableMap<Int, JPanel> = HashMap()

    override fun getListCellRendererComponent(list: PanelList,
                                              value: ListViewItem<*>,
                                              index: Int,
                                              cellHasFocus: Boolean): JPanel {


        if (panels.containsKey(index)) {
            return panels[index]!!
        }

        val panel = createPanel(value,index)

        panels[index] = panel

        return wrap(panel)
    }


    abstract fun createPanel(value: ListViewItem<*>, index: Int): JPanel


    private fun wrap(panel: JPanel): JPanel {
        return panel
    }




    override fun intervalAdded(e: ListDataEvent?) {
        panels.clear()
    }

    override fun intervalRemoved(e: ListDataEvent?) {
        panels.clear()
    }

    override fun contentsChanged(e: ListDataEvent?) {
        panels.clear()
    }


}
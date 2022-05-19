package org.digma.intellij.plugin.ui.list

import com.intellij.util.ui.JBUI.Borders
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import javax.swing.JPanel
import javax.swing.event.ListDataEvent

abstract class AbstractPanelListCellRenderer: PanelListCellRenderer {

    override fun getListCellRendererComponent(list: PanelList,
                                              value: ListViewItem<*>,
                                              index: Int,
                                              cellHasFocus: Boolean): JPanel {

        return wrap(createPanel(value,index))
    }


    abstract fun createPanel(value: ListViewItem<*>, index: Int): JPanel


    private fun wrap(component: JPanel): JPanel {

        val wrapper = JPanel()
        wrapper.layout = BorderLayout()
        wrapper.add(component,BorderLayout.CENTER)
        wrapper.border = Borders.customLine(Color.GRAY,5)

        return wrapper
//        return component
    }




    override fun intervalAdded(e: ListDataEvent?) {

    }

    override fun intervalRemoved(e: ListDataEvent?) {

    }

    override fun contentsChanged(e: ListDataEvent?) {

    }


}
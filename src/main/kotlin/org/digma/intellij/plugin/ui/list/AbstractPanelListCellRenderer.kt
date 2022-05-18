package org.digma.intellij.plugin.ui.list

import com.intellij.ui.ColorChooser
import com.intellij.ui.Colors
import com.intellij.util.ui.JBUI.Borders
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.event.ListDataEvent

abstract class AbstractPanelListCellRenderer<E: ListViewItem>: PanelListCellRenderer<ListViewItem> {

    override fun getListCellRendererComponent(list: PanelList<out ListViewItem>,
                                              value: ListViewItem,
                                              index: Int,
                                              cellHasFocus: Boolean): Component {

        return wrap(createPanel(value as E,index))
    }


    abstract fun createPanel(value: E, index: Int): Component


    private fun wrap(component: Component): Component {

        val wrapper = JPanel()
        wrapper.layout = BorderLayout()
        wrapper.add(component,BorderLayout.CENTER)
        wrapper.border = Borders.customLine(Color.DARK_GRAY,5)

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
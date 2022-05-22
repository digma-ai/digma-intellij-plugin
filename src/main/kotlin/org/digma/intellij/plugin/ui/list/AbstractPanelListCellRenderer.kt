package org.digma.intellij.plugin.ui.list

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI.Borders
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.border.BevelBorder
import javax.swing.event.ListDataEvent

abstract class AbstractPanelListCellRenderer: PanelListCellRenderer {

    override fun getListCellRendererComponent(list: PanelList,
                                              value: ListViewItem<*>,
                                              index: Int,
                                              cellHasFocus: Boolean): JPanel {

        return wrap(createPanel(value,index))
    }


    abstract fun createPanel(value: ListViewItem<*>, index: Int): JPanel


    private fun wrap(panel: JPanel): JPanel {

        return panel

//        panel.border = BorderFactory.createEtchedBorder()
//        panel.border = BorderFactory.createRaisedBevelBorder()
//        panel.border = BorderFactory.createMatteBorder(1,5,1,1,Color.BLACK)

//        val wrapper = JPanel()
//
////        val raisedBevel = BorderFactory.createRaisedBevelBorder()
////        val loweredBevel = BorderFactory.createLoweredBevelBorder()
////        val compound = BorderFactory.createCompoundBorder(raisedBevel,loweredBevel)
//
//
//        wrapper.layout = BorderLayout()
//        wrapper.add(panel,BorderLayout.CENTER)
////        wrapper.border = compound
////        wrapper.border = Borders.customLine(Color.GRAY,5)
//        wrapper.border = BorderFactory.createRaisedBevelBorder()
////        wrapper.border = BorderFactory.createMatteBorder(1,10,1,1,Color.)
//
//        return wrapper
//
//        return wrapper
    }




    override fun intervalAdded(e: ListDataEvent?) {

    }

    override fun intervalRemoved(e: ListDataEvent?) {

    }

    override fun contentsChanged(e: ListDataEvent?) {

    }


}
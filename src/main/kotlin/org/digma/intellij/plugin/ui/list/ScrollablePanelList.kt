package org.digma.intellij.plugin.ui.list

import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JPanel

class ScrollablePanelList(private val panelList: PanelList) : JPanel() {

    init {
        this.layout = BorderLayout()
        val scrollPane = JBScrollPane()
//        scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
        scrollPane.isOpaque = true
        scrollPane.background = Color.PINK
        panelList.setScrollablePanelListPanel(this)
        scrollPane.setViewportView(panelList)
        this.add(scrollPane,BorderLayout.CENTER)
//        this.border = BorderFactory.createLoweredBevelBorder()
    }



    fun getModel(): PanelListModel {
        return panelList.getModel()
    }

}
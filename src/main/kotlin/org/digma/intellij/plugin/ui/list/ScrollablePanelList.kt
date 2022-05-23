package org.digma.intellij.plugin.ui.list

import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.JPanel

class ScrollablePanelList(private val panelList: PanelList) : JPanel() {

    init {
        this.layout = BorderLayout()
        val scrollPane = JBScrollPane()
        scrollPane.isOpaque = true
        scrollPane.background = Color.PINK
        panelList.setScrollablePanelListPanel(this)
        scrollPane.setViewportView(panelList)
        scrollPane.border = BorderFactory.createEmptyBorder()
        this.add(scrollPane, BorderLayout.CENTER)
        this.border = BorderFactory.createEmptyBorder()
        this.isOpaque = true
    }



    fun getModel(): PanelListModel {
        return panelList.getModel()
    }

}
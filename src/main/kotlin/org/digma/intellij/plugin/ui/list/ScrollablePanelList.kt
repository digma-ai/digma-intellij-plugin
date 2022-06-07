package org.digma.intellij.plugin.ui.list

import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI.Borders.customLine
import com.intellij.util.ui.JBUI.Borders.empty
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JPanel

class ScrollablePanelList(private val panelList: PanelList) : JPanel() {

    init {
        this.layout = BorderLayout()
        val scrollPane = JBScrollPane()
        scrollPane.isOpaque = false
        panelList.setScrollablePanelListPanel(this)
        scrollPane.setViewportView(panelList)
        scrollPane.border = empty()
        this.add(scrollPane, BorderLayout.CENTER)
        this.border = customLine(Color.BLACK,2,0,2,0)
        this.isOpaque = false
    }



    fun getModel(): PanelListModel {
        return panelList.getModel()
    }

}
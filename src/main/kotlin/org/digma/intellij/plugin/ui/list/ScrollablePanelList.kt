package org.digma.intellij.plugin.ui.list

import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI.Borders.empty
import java.awt.BorderLayout
import java.awt.Insets
import javax.swing.JPanel

class ScrollablePanelList(private val panelList: PanelList) : JPanel() {

    private val scrollPane = JBScrollPane()
    init {
        this.layout = BorderLayout()

        scrollPane.isOpaque = false
        panelList.setScrollablePanelListPanel(this)
        scrollPane.setViewportView(panelList)
        scrollPane.border = empty()
        this.add(scrollPane, BorderLayout.CENTER)
        this.border = empty()
        this.isOpaque = false
    }


    override fun getInsets(): Insets {
        return Insets(0,0,0,0)
    }

    fun getModel(): PanelListModel {
        return panelList.getModel()
    }

}
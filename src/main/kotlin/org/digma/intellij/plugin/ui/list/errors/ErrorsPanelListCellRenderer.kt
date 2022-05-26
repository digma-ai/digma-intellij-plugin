package org.digma.intellij.plugin.ui.list.errors

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel


class ErrorsPanelListCellRenderer : AbstractPanelListCellRenderer() {


    override fun createPanel(project: Project, value: ListViewItem<*>, index: Int): JPanel {
        return getOrCreatePanel(project, index, value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getOrCreatePanel(project: Project, index: Int, value: ListViewItem<*>): JPanel {

        val panel = JPanel()
        panel.layout = BorderLayout()
        val label = JLabel()
        label.text = "this is an error $index"
        panel.add(label,BorderLayout.CENTER)

        return panel
    }

}
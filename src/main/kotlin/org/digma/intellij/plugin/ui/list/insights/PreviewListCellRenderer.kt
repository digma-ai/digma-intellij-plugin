package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummary
import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import javax.swing.JPanel


class PreviewListCellRenderer : AbstractPanelListCellRenderer() {


    override fun createPanel(project: Project, value: ListViewItem<*>, index: Int): JPanel {
        return getOrCreatePanel(project, index, value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getOrCreatePanel(project: Project, index: Int, value: ListViewItem<*>): JPanel {

        return previewPanel(value as ListViewItem<CodeObjectSummary>)
    }


}
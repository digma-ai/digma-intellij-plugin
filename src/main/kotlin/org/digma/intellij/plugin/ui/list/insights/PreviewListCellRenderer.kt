package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.service.InsightsActionsService
import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import javax.swing.JPanel


class PreviewListCellRenderer : AbstractPanelListCellRenderer() {


    override fun createPanel(project: Project,
                             value: ListViewItem<*>,
                             index: Int,
                             panelsLayoutHelper: PanelsLayoutHelper): JPanel {
        return getOrCreatePanel(project, index, value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getOrCreatePanel(project: Project, index: Int, value: ListViewItem<*>): JPanel {
        return previewPanel(project,value as ListViewItem<String>)
    }


}


fun previewPanel(project: Project, listViewItem: ListViewItem<String>): JPanel {

    val methodId = listViewItem.modelObject

    return panel {
        row {
            link(methodId.substringAfterLast("\$_\$",methodId)){
                val actionListener: InsightsActionsService = project.getService(InsightsActionsService::class.java)
                actionListener.navigateToMethod(methodId)
            }.applyToComponent {
                toolTipText = methodId
            }
        }
    }.withBackground(listBackground())
}

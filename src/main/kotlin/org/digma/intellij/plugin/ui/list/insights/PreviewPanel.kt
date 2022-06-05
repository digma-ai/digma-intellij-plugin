package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummary
import org.digma.intellij.plugin.service.InsightsActionsService
import org.digma.intellij.plugin.ui.list.panelListBackground
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import javax.swing.JPanel


fun previewPanel(project: Project, listViewItem: ListViewItem<CodeObjectSummary>): JPanel {

    val codeObjectSummary = listViewItem.modelObject

    val result = panel {
        row {
            link(codeObjectSummary.codeObjectId.replace("\$_\$",".")){
                val actionListener: InsightsActionsService = project.getService(InsightsActionsService::class.java)
                actionListener.navigateToMethod(codeObjectSummary.codeObjectId)
            }
        }
    }

    result.background = panelListBackground()
    return result

}

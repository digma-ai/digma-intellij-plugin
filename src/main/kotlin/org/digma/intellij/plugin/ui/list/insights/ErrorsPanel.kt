package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI.Borders
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight
import org.digma.intellij.plugin.model.rest.insights.ErrorInsightNamedError
import org.digma.intellij.plugin.service.ErrorsActionsService
import org.digma.intellij.plugin.service.InsightsActionsService
import org.digma.intellij.plugin.ui.common.*
import org.digma.intellij.plugin.ui.list.ListItemActionButton
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.*

fun errorsPanel(project: Project, listViewItem: ListViewItem<ErrorInsight>, panelsLayoutHelper: PanelsLayoutHelper): JPanel {

    val errorCount = listViewItem.modelObject.errorCount
    val unhandled = listViewItem.modelObject.unhandledCount
    val unexpected = listViewItem.modelObject.unexpectedCount
    val title = JLabel(buildBoldTitleGrayedComment("Errors","$errorCount errors($unhandled unhandled, $unexpected unexpected)"),
                                SwingConstants.LEFT)


    val errorsListPanel = JPanel()
    errorsListPanel.layout = GridLayout(listViewItem.modelObject.topErrors.size, 1,0,3)
    errorsListPanel.border = Borders.empty()
    errorsListPanel.isOpaque = false
    listViewItem.modelObject.topErrors.forEach { error: ErrorInsightNamedError ->

        val from = if (listViewItem.modelObject.codeObjectId != error.sourceCodeObjectId) {
            error.sourceCodeObjectId.split("\$_\$")[1]
        }else "me"
        val errorText = buildLinkTextWithGrayedAndDefaultLabelColorPart(error.errorType ,"From", from)
        val link = ActionLink(errorText) {
            val actionListener: ErrorsActionsService = project.getService(ErrorsActionsService::class.java)
            actionListener.showErrorDetails(error)
        }
        link.toolTipText = errorText
        errorsListPanel.add(link)
    }

    val errorsWrapper = JBPanel<JBPanel<*>>()
    errorsWrapper.layout = BorderLayout(0,10)
    errorsWrapper.border = BorderFactory.createEmptyBorder()
    errorsWrapper.isOpaque = false
    errorsWrapper.add(title, BorderLayout.NORTH)
    errorsWrapper.add(errorsListPanel, BorderLayout.CENTER)


    val expandButton = ListItemActionButton("Expand")
    expandButton.addActionListener { project.getService(InsightsActionsService::class.java).showErrorsTab(listViewItem.modelObject) }

    val expandPanel = JPanel()
    expandPanel.layout = BorderLayout()
    expandPanel.isOpaque = false
    expandPanel.add(expandButton, BorderLayout.EAST)

    val result = JBPanel<JBPanel<*>>()
    result.layout = BorderLayout()
    result.isOpaque = false
    result.add(errorsWrapper,BorderLayout.CENTER)
    result.add(expandPanel,BorderLayout.SOUTH)

    return insightItemPanel(result)
}

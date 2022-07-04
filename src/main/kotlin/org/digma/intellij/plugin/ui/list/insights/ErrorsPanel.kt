package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBUI.Borders
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight
import org.digma.intellij.plugin.model.rest.insights.ErrorInsightNamedError
import org.digma.intellij.plugin.service.ErrorsActionsService
import org.digma.intellij.plugin.service.InsightsActionsService
import org.digma.intellij.plugin.ui.common.*
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.*

fun errorsPanel(project: Project, listViewItem: ListViewItem<ErrorInsight>): JPanel {

    val errorCount = listViewItem.modelObject.errorCount
    val unhandled = listViewItem.modelObject.unhandledCount
    val unexpected = listViewItem.modelObject.unexpectedCount
    val title = JLabel(buildBoldTitleGrayedComment("Errors","$errorCount errors($unhandled unhandled, $unexpected unexpected)"),
                                SwingConstants.LEFT)


    val errorsListPanel = JPanel()
    errorsListPanel.layout = GridLayout(listViewItem.modelObject.topErrors.size, 1,0,3)
    errorsListPanel.border = Borders.empty()
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


    val expandLinkPanel = panel {
        row {
            link("Expand") {
                val actionListener: InsightsActionsService = project.getService(InsightsActionsService::class.java)
                actionListener.showErrorsTab(listViewItem.modelObject)
            }.horizontalAlign(HorizontalAlign.RIGHT)
        }.layout(RowLayout.INDEPENDENT)
    }
    expandLinkPanel.border = Borders.empty(0,0,0,10)


    val errorsWrapper = JBPanel<JBPanel<*>>()
    errorsWrapper.layout = BorderLayout(0,10)
    errorsWrapper.add(title, BorderLayout.NORTH)
    errorsWrapper.add(errorsListPanel, BorderLayout.CENTER)
    errorsWrapper.border = BorderFactory.createEmptyBorder()


    //the expand button wants to be aligned with the insights icons panels, so it takes its width from there
    val expandPanel: JPanel = object: JPanel(){
        override fun getPreferredSize(): Dimension {
            val ps = super.getPreferredSize()
            return Dimension(InsightsPanelsLayoutHelper.getObjectAttribute("insightsIconPanelBorder","largestWidth") as Int,ps.height)
        }
    }
    expandPanel.layout = BorderLayout()
    expandPanel.add(expandLinkPanel, BorderLayout.SOUTH)

    val result = JBPanel<JBPanel<*>>()
    result.layout = BorderLayout()
    result.add(errorsWrapper,BorderLayout.CENTER)
    result.add(expandPanel,BorderLayout.EAST)

    return insightItemPanel(result)
}

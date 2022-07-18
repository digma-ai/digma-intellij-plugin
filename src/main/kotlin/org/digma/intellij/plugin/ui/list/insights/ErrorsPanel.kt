package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBUI.Borders
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight
import org.digma.intellij.plugin.model.rest.insights.ErrorInsightNamedError
import org.digma.intellij.plugin.service.ErrorsActionsService
import org.digma.intellij.plugin.service.InsightsActionsService
import org.digma.intellij.plugin.ui.common.*
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

fun errorsPanel(project: Project, listViewItem: ListViewItem<ErrorInsight>, panelsLayoutHelper: PanelsLayoutHelper): JPanel {

    val errorCount = listViewItem.modelObject.errorCount
    val unhandled = listViewItem.modelObject.unhandledCount
    val unexpected = listViewItem.modelObject.unexpectedCount
    val title = JLabel(buildBoldTitleGrayedComment("Errors","$errorCount errors($unhandled unhandled, $unexpected unexpected)"),
                                SwingConstants.LEFT)


    val errorsListPanel = JPanel()
    errorsListPanel.layout = GridLayout(listViewItem.modelObject.topErrors.size, 1,0,3)
    errorsListPanel.border = Borders.empty()
    errorsListPanel.background = Intellij.LIST_ITEM_BACKGROUND
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
    errorsWrapper.background = Intellij.LIST_ITEM_BACKGROUND
    errorsWrapper.add(title, BorderLayout.NORTH)
    errorsWrapper.add(errorsListPanel, BorderLayout.CENTER)

    //the expand link button needs to be the same size as insightsIconPanel so that they will align
    //the same over the list
    val expandLinkPanel = panel {
        row {
            link("Expand") {
                val actionListener: InsightsActionsService = project.getService(InsightsActionsService::class.java)
                actionListener.showErrorsTab(listViewItem.modelObject)
            }.horizontalAlign(HorizontalAlign.CENTER)
        }
    }
    expandLinkPanel.border = Borders.empty(0,0,0,Laf.scaleBorders(getInsightIconPanelRightBorderSize()))
    expandLinkPanel.background = Intellij.LIST_ITEM_BACKGROUND

    //the expand button wants to be aligned with the insights icons panels, so it takes its width from there
    val expandPanel: JPanel = object: JPanel(){
        override fun getPreferredSize(): Dimension {
            val ps = super.getPreferredSize()
            if (ps == null) {
                return ps
            }
            val h = ps.height
            val w = ps.width
            addCurrentLargestWidthIconPanel(panelsLayoutHelper,w)
            return Dimension(getCurrentLargestWidthIconPanel(panelsLayoutHelper,w), h)
        }
    }
    expandPanel.layout = BorderLayout()
    expandPanel.background = Intellij.LIST_ITEM_BACKGROUND
    expandPanel.add(expandLinkPanel, BorderLayout.SOUTH)
    addCurrentLargestWidthIconPanel(panelsLayoutHelper,expandPanel.preferredSize?.width ?: 0)

    val result = JBPanel<JBPanel<*>>()
    result.layout = BorderLayout()
    result.background = Intellij.LIST_ITEM_BACKGROUND
    result.add(errorsWrapper,BorderLayout.CENTER)
    result.add(expandPanel,BorderLayout.EAST)

    return insightItemPanel(result)
}

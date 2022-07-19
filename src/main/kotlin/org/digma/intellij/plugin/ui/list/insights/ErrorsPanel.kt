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
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.buildBoldTitleGrayedComment
import org.digma.intellij.plugin.ui.common.buildLinkTextWithGrayedAndDefaultLabelColorPart
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

fun errorsPanel(project: Project, modelObject: ErrorInsight, panelsLayoutHelper: PanelsLayoutHelper): JPanel {

    val errorCount = modelObject.errorCount
    val unhandled = modelObject.unhandledCount
    val unexpected = modelObject.unexpectedCount
    val title = JLabel(buildBoldTitleGrayedComment("Errors","$errorCount errors($unhandled unhandled, $unexpected unexpected)"),
                                SwingConstants.LEFT)


    val errorsListPanel = JPanel()
    errorsListPanel.layout = GridLayout(modelObject.topErrors.size, 1,0,3)
    errorsListPanel.border = Borders.empty()
    modelObject.topErrors.forEach { error: ErrorInsightNamedError ->

        val from = if (modelObject.codeObjectId != error.sourceCodeObjectId) {
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
    errorsWrapper.add(title, BorderLayout.NORTH)
    errorsWrapper.add(errorsListPanel, BorderLayout.CENTER)
    errorsWrapper.border = BorderFactory.createEmptyBorder()



    //the expand link button needs to be the same size as insightsIconPanel so that they will align
    //the same over the list
    val expandLinkPanel = panel {
        row {
            link("Expand") {
                val actionListener: InsightsActionsService = project.getService(InsightsActionsService::class.java)
                actionListener.showErrorsTab(modelObject)
            }.horizontalAlign(HorizontalAlign.CENTER)
        }
    }
    expandLinkPanel.border = Borders.empty(0,0,0,Laf.scaleBorders(getInsightIconPanelRightBorderSize()))

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
    expandPanel.add(expandLinkPanel, BorderLayout.SOUTH)
    addCurrentLargestWidthIconPanel(panelsLayoutHelper,expandPanel.preferredSize?.width ?: 0)

    val result = JBPanel<JBPanel<*>>()
    result.layout = BorderLayout()
    result.add(errorsWrapper,BorderLayout.CENTER)
    result.add(expandPanel,BorderLayout.EAST)

    return insightItemPanel(result)
}

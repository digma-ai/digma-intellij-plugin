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
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.htmlSpanSmoked
import org.digma.intellij.plugin.ui.common.htmlSpanTitle
import org.digma.intellij.plugin.ui.common.htmlSpanWhite
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.*

fun errorsPanel(project: Project, listViewItem: ListViewItem<ErrorInsight>): JPanel {

    val errorCount = listViewItem.modelObject.errorCount
    val unhandled = listViewItem.modelObject.unhandledCount
    val unexpected = listViewItem.modelObject.unexpectedCount
    val title = JLabel(asHtml("${htmlSpanTitle()}<b>Errors</b><br> " +
                                "${htmlSpanSmoked()}$errorCount errors($unhandled unhandled, $unexpected unexpected)"),
                                SwingConstants.LEFT)


    val errorsListPanel = JPanel()
    errorsListPanel.layout = GridLayout(listViewItem.modelObject.topErrors.size, 1,0,3)
    errorsListPanel.border = Borders.empty()
    listViewItem.modelObject.topErrors.forEach { error: ErrorInsightNamedError ->

        var errorText = "<b>${error.errorType}</b> "
        var from = "${htmlSpanSmoked()}From me"
        if (listViewItem.modelObject.codeObjectId != error.sourceCodeObjectId) {
            from = "${htmlSpanSmoked()}From ${htmlSpanWhite()}${error.sourceCodeObjectId.split("\$_\$")[1]}"
        }
        errorText = asHtml(errorText + from)
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
    expandLinkPanel.border = Borders.empty()


    val errorsWrapper = JBPanel<JBPanel<*>>()
    errorsWrapper.layout = BorderLayout(0,10)
    errorsWrapper.add(title, BorderLayout.NORTH)
    errorsWrapper.add(errorsListPanel, BorderLayout.CENTER)
    errorsWrapper.border = BorderFactory.createEmptyBorder()


    val expandPanel = JBPanel<JBPanel<*>>()
    expandPanel.layout = BorderLayout()
    expandPanel.add(expandLinkPanel, BorderLayout.SOUTH)


    val result = JBPanel<JBPanel<*>>()
    result.layout = BoxLayout(result, BoxLayout.X_AXIS)
    result.add(errorsWrapper)
    result.add(Box.createHorizontalStrut(5))
    result.add(expandPanel)

    return insightItemPanel(result)
}

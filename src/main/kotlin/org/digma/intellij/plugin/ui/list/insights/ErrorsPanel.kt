package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI.Borders
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight
import org.digma.intellij.plugin.model.rest.insights.ErrorInsightNamedError
import org.digma.intellij.plugin.service.ErrorsActionsService
import org.digma.intellij.plugin.service.InsightsActionsService
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.buildLinkTextWithGrayedAndDefaultLabelColorPart
import org.digma.intellij.plugin.ui.list.ListItemActionButton
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import java.awt.GridLayout
import javax.swing.JPanel

fun errorsPanel(project: Project, modelObject: ErrorInsight, panelsLayoutHelper: PanelsLayoutHelper): JPanel {

    val errorCount = modelObject.errorCount
    val unhandled = modelObject.unhandledCount
    val unexpected = modelObject.unexpectedCount

    val errorsListPanel = JPanel()
    errorsListPanel.layout = GridLayout(modelObject.topErrors.size, 1,0,3)
    errorsListPanel.border = Borders.empty()
    errorsListPanel.isOpaque = false
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

    val expandButton = ListItemActionButton("Expand")
    expandButton.addActionListener { project.getService(InsightsActionsService::class.java).showErrorsTab(modelObject) }

    return createInsightPanel(
        "Errors",
        "$errorCount errors($unhandled unhandled, $unexpected unexpected)",
        Laf.Icons.Insight.ERRORS,
        errorsListPanel,
        listOf(expandButton),
        panelsLayoutHelper)

    //return insightItemPanel(result)
}

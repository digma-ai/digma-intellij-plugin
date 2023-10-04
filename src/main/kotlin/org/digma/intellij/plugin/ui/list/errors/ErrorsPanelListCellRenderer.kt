package org.digma.intellij.plugin.ui.list.errors

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.common.CommonUtils.prettyTimeOf
import org.digma.intellij.plugin.insights.ErrorsViewOrchestrator
import org.digma.intellij.plugin.model.discovery.CodeObjectInfo.Companion.extractMethodName
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.ui.common.CopyableLabelHtml
import org.digma.intellij.plugin.ui.common.TraceButton
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.buildLinkTextWithGrayedAndDefaultLabelColorPart
import org.digma.intellij.plugin.ui.common.createScorePanelNoArrows
import org.digma.intellij.plugin.ui.common.span
import org.digma.intellij.plugin.ui.common.spanGrayed
import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.list.commonListItemPanel
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.sql.Timestamp
import javax.swing.JPanel


class ErrorsPanelListCellRenderer : AbstractPanelListCellRenderer() {


    @Suppress("UNCHECKED_CAST")
    override fun createPanel(
        project: Project,
        value: ListViewItem<*>,
        index: Int,
        panelsLayoutHelper: PanelsLayoutHelper,
    ): JPanel {
        return getOrCreatePanel(project, value as ListViewItem<CodeObjectError>)
    }

    private fun getOrCreatePanel(project: Project, value: ListViewItem<CodeObjectError>): JPanel {
        val model = value.modelObject
        return commonListItemPanel(createSingleErrorPanel(project, model))
    }

}

private fun createSingleErrorPanel(project: Project, model: CodeObjectError): JPanel {

    val relativeFrom = if (model.startsHere) {
        "me"
    } else {
        extractMethodName(model.sourceCodeObjectId)
    }

    val linkText = buildLinkTextWithGrayedAndDefaultLabelColorPart(model.name, "from", relativeFrom)
    val link = ActionLink(asHtml(linkText)) {
        project.service<ErrorsViewOrchestrator>().showErrorDetails(model)
    }

    val firstAndLast = contentOfFirstAndLast(model.firstOccurenceTime, model.lastOccurenceTime)

    link.toolTipText = asHtml("${linkText}<br>${firstAndLast}")

    val contentText = "${span(model.characteristic)}<br> $firstAndLast"
    val content = CopyableLabelHtml(asHtml(contentText))


    val scorePanel = createScorePanelNoArrows(model)
    val scorePanelWrapper = JPanel()
    scorePanelWrapper.border = JBUI.Borders.emptyRight(5)
    scorePanelWrapper.isOpaque = false
    scorePanelWrapper.layout = GridBagLayout()
    val constraints = GridBagConstraints()
    constraints.anchor = GridBagConstraints.NORTH
    scorePanelWrapper.add(scorePanel)

    val leftPanel = JBPanel<JBPanel<*>>()
    leftPanel.layout = BorderLayout(0, 3)
    leftPanel.isOpaque = false
    leftPanel.border = JBUI.Borders.emptyRight(10)
    leftPanel.add(link, BorderLayout.NORTH)
    leftPanel.add(content, BorderLayout.CENTER)

    var traceButton: TraceButton? = null
    if (model.latestTraceId != null && "NA" != model.latestTraceId) {
        val title = "Sample trace for error ${model.name}"
        traceButton = TraceButton()
        traceButton.defineAction(project, model.latestTraceId!!, title)
    }

    val buttonsPanel = JBPanel<JBPanel<*>>()
    buttonsPanel.layout = BorderLayout(0, 3)
    buttonsPanel.isOpaque = false
    buttonsPanel.border = JBUI.Borders.emptyRight(5)
    if (traceButton != null) {
        buttonsPanel.add(traceButton, BorderLayout.EAST)
    }

    val result = JPanel()
    result.layout = BorderLayout(0, 3)
    result.isOpaque = false
    result.add(leftPanel, BorderLayout.CENTER)
    result.add(scorePanelWrapper, BorderLayout.EAST)
    result.add(buttonsPanel, BorderLayout.SOUTH)
    return result
}

fun contentOfFirstAndLast(firstOccurenceTime: Timestamp, lastOccurenceTime: Timestamp): String {
    return "${spanGrayed("Started:")} ${span(prettyTimeOf(firstOccurenceTime))}" +
            "  ${spanGrayed("Last:")} ${span(prettyTimeOf(lastOccurenceTime))}"
}

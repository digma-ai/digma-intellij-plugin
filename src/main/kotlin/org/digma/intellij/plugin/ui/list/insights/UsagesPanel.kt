package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.model.rest.insights.SpanFlow
import org.digma.intellij.plugin.model.rest.insights.SpanUsagesInsight
import org.digma.intellij.plugin.ui.common.*
import org.digma.intellij.plugin.ui.list.openWorkspaceFileForSpan
import org.digma.intellij.plugin.ui.model.TraceSample
import java.awt.BorderLayout
import javax.swing.*

fun spanUsagesPanel(project: Project, spanUsagesInsight: SpanUsagesInsight, moreData: HashMap<String, Any>): JPanel {

    val title = JLabel(asHtml(spanBold("Top Usage")), SwingConstants.LEFT)
    title.isOpaque = false

    val flowsListPanel = JBPanel<JBPanel<*>>()
    flowsListPanel.layout = BoxLayout(flowsListPanel, BoxLayout.Y_AXIS)
    flowsListPanel.border = JBUI.Borders.empty()
    flowsListPanel.isOpaque = false

    spanUsagesInsight.flows.forEach { spanFlow: SpanFlow ->

        val line = createDefaultBoxLayoutLineAxisPanel()
        line.isOpaque = false

        val percentageLabel = CopyableLabelHtml(asHtml("${span(String.format("%.1f", spanFlow.percentage))}% "))
        percentageLabel.border = JBUI.Borders.emptyRight(5)

        line.add(percentageLabel, BorderLayout.WEST)

        val builder = StringBuilder()
        var spanName = spanUsagesInsight.span // default, just in case first service is not found
        spanFlow.firstService?.let { firstService ->
            line.add(CopyableLabelHtml(asHtml(spanGrayed(firstService.service + ": "))))
            addSpanLinkIfPossible(project, firstService, moreData, line)
            spanName = firstService.span
        }
        spanFlow.intermediateSpan?.let { intermediateSpan ->
            line.add(CopyableLabelHtml(asHtml(" ${spanGrayed(Html.ARROW_RIGHT)} ${span(intermediateSpan)}")))
        }
        spanFlow.lastService?.let { lastService ->
            line.add(CopyableLabelHtml(asHtml(" ${spanGrayed(Html.ARROW_RIGHT)} ${spanGrayed(lastService.service + ": ")}")))
            addSpanLinkIfPossible(project, lastService, moreData, line)
        }
        spanFlow.lastServiceSpan?.let { lastServiceSpan ->
            line.add(CopyableLabelHtml(asHtml(" ${spanGrayed(Html.ARROW_RIGHT)} ${span(lastServiceSpan)}")))
        }
        var traceSample: TraceSample? = null
        spanFlow.sampleTraceIds.firstOrNull()?.let { sampleTraceId ->
            traceSample = TraceSample(spanName, sampleTraceId)
        }
        flowsListPanel.add(line)
        buildJPanelWithButtonToJaeger(builder, flowsListPanel, traceSample, project, spanName)
    }

    val result = JBPanel<JBPanel<*>>()
    result.layout = BorderLayout()
    result.add(title, BorderLayout.NORTH)
    result.add(flowsListPanel, BorderLayout.CENTER)

    return insightItemPanel(result)
}

fun addSpanLinkIfPossible(project: Project, service: SpanFlow.Service, moreData: HashMap<String, Any>, panel: JPanel) {
    if (moreData.contains(service.codeObjectId)){
        var spanName = service.span;
        val link = ActionLink(spanName) {
            openWorkspaceFileForSpan(project, moreData, service.codeObjectId)
        }
        var targetClass = service.codeObjectId.substringBeforeLast("\$_\$");

        link.toolTipText = asHtml("$targetClass: $spanName")
        panel.add(link)
    }
}
package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.model.rest.insights.SpanFlow
import org.digma.intellij.plugin.model.rest.insights.SpanUsagesInsight
import org.digma.intellij.plugin.ui.common.*
import org.digma.intellij.plugin.ui.model.TraceSample
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

fun spanUsagesPanel(project: Project, spanUsagesInsight: SpanUsagesInsight): JPanel {

    val title = JLabel(asHtml(spanBold("Top Usage")), SwingConstants.LEFT)
    title.isOpaque = false

    val flowsListPanel = JBPanel<JBPanel<*>>()
    flowsListPanel.layout = BoxLayout(flowsListPanel, BoxLayout.Y_AXIS)
    flowsListPanel.border = JBUI.Borders.empty()
    flowsListPanel.isOpaque = false

    spanUsagesInsight.flows.forEach { spanFlow: SpanFlow ->

        val line = JPanel(BorderLayout())
        line.isOpaque = false

        val percentageLabel = CopyableLabelHtml(asHtml("${span(String.format("%.1f", spanFlow.percentage))}% "))
        percentageLabel.border = JBUI.Borders.emptyRight(5)

        line.add(percentageLabel, BorderLayout.WEST)

        val builder = StringBuilder()
        var spanName = spanUsagesInsight.span // default, just in case first service is not found
        spanFlow.firstService?.let { firstService ->
            builder.append(spanGrayed(firstService.service + ": "))
            builder.append(span(firstService.span))
            spanName = firstService.span
        }
        spanFlow.intermediateSpan?.let { intermediateSpan ->
            builder.append(" ${spanGrayed(Html.ARROW_RIGHT)} ")
            builder.append(span(intermediateSpan))
        }
        spanFlow.lastService?.let { lastService ->
            builder.append(" ${spanGrayed(Html.ARROW_RIGHT)} ")
            builder.append(spanGrayed(lastService.service + ": "))
            builder.append(span(lastService.span))
        }
        spanFlow.lastServiceSpan?.let { lastServiceSpan ->
            builder.append(" ${spanGrayed(Html.ARROW_RIGHT)} ")
            builder.append(span(lastServiceSpan))
        }
        var traceSample: TraceSample? = null
        spanFlow.sampleTraceIds.firstOrNull()?.let { sampleTraceId ->
            traceSample = TraceSample(spanName, sampleTraceId)
        }
        buildJPanelWithButtonToJaeger(builder, line, traceSample, project, spanName)
        flowsListPanel.add(line)
    }

    val result = JBPanel<JBPanel<*>>()
    result.layout = BorderLayout()
    result.add(title, BorderLayout.NORTH)
    result.add(flowsListPanel, BorderLayout.CENTER)

    return insightItemPanel(result)
}
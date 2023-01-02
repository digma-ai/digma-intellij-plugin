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
import javax.swing.JButton
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
        percentageLabel.border = JBUI.Borders.empty(0, 0, 0, 5)

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
        val spanFlowLabel = CopyableLabelHtml(asHtml(builder.toString()))
        spanFlowLabel.alignmentX = 0.0f
        line.add(spanFlowLabel, BorderLayout.CENTER)

        var traceSample: TraceSample? = null
        spanFlow.sampleTraceIds.firstOrNull()?.let { sampleTraceId ->
            traceSample = TraceSample(spanName, sampleTraceId)
        }
        val buttonToJaeger = buildButtonToJaeger(project, "Trace", spanName, traceSample)
        if (buttonToJaeger != null) {
            val wrapper = JPanel(BorderLayout())
            wrapper.isOpaque = false
            wrapper.add(buttonToJaeger, BorderLayout.NORTH)
            line.add(wrapper, BorderLayout.EAST)
        }
        flowsListPanel.add(line)
    }

    val result = JBPanel<JBPanel<*>>()
    result.layout = BorderLayout()
    result.add(title, BorderLayout.NORTH)
    result.add(flowsListPanel, BorderLayout.CENTER)

    return insightItemPanel(result)
}

// if cannot create the button then would return null
private fun buildButtonToJaeger(
        project: Project, linkCaption: String, spanName: String, traceSample: TraceSample?
): JButton? {
    if (traceSample == null) {
        return null
    }
    return buildButtonToJaeger(project, linkCaption, spanName, listOf(traceSample))
}
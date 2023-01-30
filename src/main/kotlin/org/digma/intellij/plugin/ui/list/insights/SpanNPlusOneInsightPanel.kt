package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import org.apache.commons.lang3.StringUtils
import org.digma.intellij.plugin.model.rest.insights.SpanNPlusOneInsight
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.spanBold
import org.digma.intellij.plugin.ui.model.TraceSample
import java.awt.BorderLayout
import javax.swing.*

fun spanNPlusOneInsightPanel(project: Project, insight: SpanNPlusOneInsight): JPanel {
    val resultPanel = createDefaultBoxLayoutLineAxisPanel()
    resultPanel.add(spanNPlusOneInsightRowPanel(insight))

    return createInsightPanel(
            project = project,
            insight = insight,
            title = "Suspected N-Plus-1",
            description = asHtml("Check the following SELECT statement"),
            iconsList = listOf(Laf.Icons.Insight.N_PLUS_ONE),
            bodyPanel = resultPanel,
            buttons = listOf(getButtonToJaeger(project, insight)),
            paginationComponent = null
    )
}

private fun getButtonToJaeger(project: Project, insight: SpanNPlusOneInsight): JButton? {
    val spanName = insight.clientSpanName
    val sampleTraceId = insight.traceId
    val traceSample = spanName?.let { TraceSample(it, sampleTraceId) }
    return spanName?.let { buildButtonToJaeger(project, "Trace", it, listOf(traceSample)) }
}

private fun spanNPlusOneInsightRowPanel(insight: SpanNPlusOneInsight): JPanel {
    val resultPanel = createDefaultBoxLayoutYAxisPanel()
    resultPanel.add(getMainDescriptionPanel(insight))
    resultPanel.add(getRowPanel(insight))
    return resultPanel
}

private fun getMainDescriptionPanel(insight: SpanNPlusOneInsight): JPanel {
    val spanOneRecordPanel = getDefaultSpanOneRecordPanel()
    val displayText: String? = insight.clientSpanName
    if (StringUtils.isNotEmpty(displayText)) {
        val jbLabel = JBLabel(displayText!!, SwingConstants.TRAILING)
        jbLabel.toolTipText = asHtml(displayText)
        jbLabel.horizontalAlignment = SwingConstants.LEFT
        spanOneRecordPanel.add(jbLabel, BorderLayout.NORTH)
    }
    return spanOneRecordPanel
}

private fun getRowPanel(insight: SpanNPlusOneInsight): JPanel {
    val rowPanel = createDefaultBoxLayoutLineAxisPanel()

    val repeatsValue = "${insight.occurrences} (median)"
    val repeatsLabel = JLabel(asHtml("Repeats: ${spanBold(repeatsValue)}"))
    val durationLabel = JLabel(asHtml("Duration: " +
            spanBold("${insight.duration.value} ${insight.duration.unit}")))

    rowPanel.add(repeatsLabel)
    rowPanel.add(Box.createHorizontalGlue())
    rowPanel.add(durationLabel)
    return rowPanel
}
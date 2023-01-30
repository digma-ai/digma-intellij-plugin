package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import org.apache.commons.lang3.StringUtils
import org.digma.intellij.plugin.model.rest.insights.EPNPlusSpansInsight
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.spanBold
import org.digma.intellij.plugin.ui.model.TraceSample
import java.awt.BorderLayout
import javax.swing.Box
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

fun ePNPlusSpansPanel(project: Project, insight: EPNPlusSpansInsight): JPanel {
    val resultPanel = createDefaultBoxLayoutYAxisPanel()

    resultPanel.add(getSQLRowPanel(insight))
    resultPanel.add(getRowPanel(insight))

    val spanName = insight.endpointSpan
    val sampleTraceId = insight.spans.first().traceId
    val traceSample = TraceSample(spanName, sampleTraceId)
    val buttonToJaeger = buildButtonToJaeger(project, "Trace", spanName, listOf(traceSample))

    val result = createInsightPanel(
            project = project,
            insight = insight,
            title = "Suspected N-Plus-1",
            description = asHtml("Check the following SELECT statement"),
            iconsList = listOf(Laf.Icons.Insight.N_PLUS_ONE),
            bodyPanel = resultPanel,
            buttons = listOf(buttonToJaeger),
            paginationComponent = null
    )
    result.toolTipText = asHtml("Repeating select query pattern suggests N-Plus-One")
    return result
}

private fun getSQLRowPanel(insight: EPNPlusSpansInsight): JPanel {
    val sqlStatement = insight.spans.first().clientSpan.displayName
    val normalizedDisplayName = StringUtils.normalizeSpace(sqlStatement)
    val displayNameLabel = JBLabel(normalizedDisplayName, SwingConstants.TRAILING)
    displayNameLabel.toolTipText = asHtml(sqlStatement)
    displayNameLabel.horizontalAlignment = SwingConstants.LEFT

    val spanOneRecordPanel = getDefaultSpanOneRecordPanel()
    spanOneRecordPanel.add(displayNameLabel, BorderLayout.NORTH)
    return spanOneRecordPanel
}

private fun getRowPanel(insight: EPNPlusSpansInsight): JPanel {
    val rowPanel = createDefaultBoxLayoutLineAxisPanel()
    rowPanel.border = JBUI.Borders.emptyBottom(5)

    val repeatsValue = "${insight.spans.first().occurrences} (median)"
    val repeatsLabel = JLabel(asHtml("Repeats: ${spanBold(repeatsValue)}"))
    val durationLabel = JLabel(asHtml("Duration: " +
            spanBold("${insight.spans.first().duration.value} ${insight.spans.first().duration.unit}")))

    rowPanel.add(repeatsLabel)
    rowPanel.add(Box.createHorizontalGlue())
    rowPanel.add(durationLabel)
    return rowPanel
}
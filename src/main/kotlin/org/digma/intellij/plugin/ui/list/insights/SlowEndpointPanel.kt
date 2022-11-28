package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.model.rest.insights.Duration
import org.digma.intellij.plugin.model.rest.insights.SlowEndpointInsight
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.span
import java.math.BigDecimal
import java.math.RoundingMode
import javax.swing.JLabel
import javax.swing.JPanel


fun slowEndpointPanel(project: Project, insight: SlowEndpointInsight): JPanel {
    val bodyContents = genContent(insight)
    val durationLabel = JLabel(evalDuration(insight.median))
    val result = createInsightPanel(
            project = project,
            insight = insight,
            title = "Slow Endpoint",
            description = bodyContents,
            iconsList = listOf(Laf.Icons.Insight.SLOW),
            bodyPanel = durationLabel,
            buttons = null,
            paginationComponent = null
            )
    result.toolTipText = asHtml(genToolTip(insight))
    return result
}

fun genToolTip(insight: SlowEndpointInsight): String {
    return """
server processed 50% of requests in less than ${evalDuration(insight.endpointsMedian)}
<br>
server processed 25% of requests in higher than ${evalDuration(insight.endpointsP75)}
"""
}

fun evalDuration(duration: Duration): String {
    return "${duration.value}${duration.unit}"
}

fun genContent(insight: SlowEndpointInsight): String {
    val pctVal = computePercentageDiff(insight)
    return "On average requests are slower than other endpoints by ${span(Laf.Colors.ERROR_RED,pctVal)}"
}

fun computePercentageDiff(insight: SlowEndpointInsight): String {
    val decimal = computePercentageDiff(insight.median.raw, insight.endpointsMedianOfMedians.raw)
    return "${decimal.toPlainString()}%"
}

fun computePercentageDiff(value: Long, compare: Long): BigDecimal {
    val decimal = BigDecimal((value.toDouble() / compare.toDouble() - 1) * 100).setScale(0, RoundingMode.HALF_DOWN)
    return decimal
}

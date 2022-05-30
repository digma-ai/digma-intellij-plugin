package org.digma.intellij.plugin.ui.list.insights

import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.Duration
import org.digma.intellij.plugin.model.rest.insights.SlowEndpointInsight
import org.digma.intellij.plugin.ui.common.HtmlConsts.RED_COLOR
import org.digma.intellij.plugin.ui.common.asHtml
import java.math.BigDecimal
import java.math.RoundingMode
import javax.swing.JPanel


fun slowEndpointPanel(insight: SlowEndpointInsight): JPanel {
    val bodyContents = genContentAsHtml(insight)
    val iconText = evalDuration(insight.median)
    val result = createInsightPanel("Slow Endpoint", bodyContents, Icons.Insight.SLOW, iconText);

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

fun genContentAsHtml(insight: SlowEndpointInsight): String {
    val pctVal = computePercentageDiff(insight)
    return asHtml("On average requests are slower than other endpoints by <span style=\"color:${RED_COLOR}\">${pctVal}")
}

fun computePercentageDiff(insight: SlowEndpointInsight): String {
    val decimal = computePercentageDiff(insight.median.raw, insight.endpointsMedianOfMedians.raw)
    return "${decimal.toPlainString()}%"
}

fun computePercentageDiff(value: Long, compare: Long): BigDecimal {
    val decimal = BigDecimal((value.toDouble() / compare.toDouble() - 1) * 100).setScale(0, RoundingMode.HALF_DOWN)
    return decimal
}

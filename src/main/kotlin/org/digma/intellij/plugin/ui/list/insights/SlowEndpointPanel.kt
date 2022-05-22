package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.Duration
import org.digma.intellij.plugin.model.rest.insights.SlowEndpointInsight
import java.math.BigDecimal
import java.math.RoundingMode


fun slowEndpointPanel(insight: SlowEndpointInsight): DialogPanel {
    val result = panel {
        twoColumnsRow(
            column1 = {
                panel {
                    row {
                        label("Slow Endpoint").bold()
                    }
                    row {
                        text(genContentAsHtml(insight))
                    }
                }
            },
            column2 = {
                panel {
                    row {
                        icon(Icons.Insight.SLOW)
                    }
                    row {
                        label(evalDuration(insight.median))
                    }
                }
            }
        )
    }
    result.toolTipText = genToolTip(insight)

    return result
}

fun genToolTip(insight: SlowEndpointInsight): String {
    return """
server processed 50% of requests in less than ${evalDuration(insight.endpointsMedian)}\n
server processed 25% of requests in higher than ${evalDuration(insight.endpointsP75)}
"""
}

fun evalDuration(duration: Duration): String {
    return "${duration.value}${duration.unit}"
}

fun genContentAsHtml(insight: SlowEndpointInsight): String {
    return "<p>On average requests are slower than </p><p>other endpoints by <span style=\"color:red\">${
        computePercentageDiff(
            insight
        )
    }</span></p>"
}

fun computePercentageDiff(insight: SlowEndpointInsight): String {
    val decimal = computePercentageDiff(insight.median.raw, insight.endpointsMedianOfMedians.raw)
    return "${decimal.toPlainString()}%"
}

fun computePercentageDiff(value: Long, compare: Long): BigDecimal {
    val decimal = BigDecimal((value.toDouble() / compare.toDouble() - 1) * 100).setScale(0, RoundingMode.HALF_DOWN)
    return decimal
}

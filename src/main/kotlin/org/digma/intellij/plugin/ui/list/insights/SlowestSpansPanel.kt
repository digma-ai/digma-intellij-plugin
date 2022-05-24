package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.Percentile
import org.digma.intellij.plugin.model.rest.insights.SlowSpanInfo
import org.digma.intellij.plugin.model.rest.insights.SlowestSpansInsight
import org.digma.intellij.plugin.ui.common.asHtml
import java.math.BigDecimal
import java.math.RoundingMode

fun slowestSpansPanel(insight: SlowestSpansInsight): DialogPanel {
    val result = panel {
        twoColumnsRow(
            column1 = {
                panel {
                    row {
                        label("Span Bottleneck").bold()
                    }
                    row {
                        label("The following spans are slowing request handling")
                    }
                }
            },
            column2 = {
                panel {
                    row {
                        icon(Icons.Insight.BOTTLENECK)
                    }
                    row {
                        label("Slow Spans")
                    }
                }
            }
        )

        insight.spans.forEach {
            val displayName = it.spanInfo.displayName
            row {
                label(displayName).bold()
            }
            row {
                label(descriptionOf(it))
            }.contextHelp(genToolTip(it), displayName)
        }
    }

    return result
}


fun descriptionOf(span: SlowSpanInfo): String {
    if (span.p50.fraction > 0.4)
        return "50% of the users by up to ${span.p50.maxDuration.value}${span.p50.maxDuration.unit}"

    if (span.p95.fraction > 0.4)
        return "5% of the users by up to ${span.p95.maxDuration.value}${span.p95.maxDuration.unit}"

    return "1% of the users by up to ${span.p99.maxDuration.value}${span.p99.maxDuration.unit}"
}

fun genToolTip(span: SlowSpanInfo): String {
    return asHtml(
        """
Percentage of time spent in span:
<pre>
Median: ${oneLiner(span.p50)}%
P95:    ${oneLiner(span.p95)}%
P99:    ${oneLiner(span.p99)}%
</pre>
"""
    )
}

private fun percentageForDisplay(percentile: Percentile): String {
    val decimal = BigDecimal(percentile.fraction * 100).setScale(0, RoundingMode.HALF_DOWN)
    return decimal.toPlainString()
}

private fun oneLiner(percentile: Percentile): String {
    return "${percentageForDisplay(percentile)}% ~${percentile.maxDuration.value}${percentile.maxDuration.unit}"
}

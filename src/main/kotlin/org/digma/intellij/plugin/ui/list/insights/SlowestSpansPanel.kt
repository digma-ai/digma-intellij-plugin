package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.Percentile
import org.digma.intellij.plugin.model.rest.insights.SlowSpanInfo
import org.digma.intellij.plugin.model.rest.insights.SlowestSpansInsight
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.wrapCentered
import java.math.BigDecimal
import java.math.RoundingMode
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel

fun slowestSpansPanel(insight: SlowestSpansInsight): JPanel {

    val topContents = createInsightPanel(
        "Span Bottleneck", asHtml("The following spans are slowing request handling"),
        Icons.Insight.BOTTLENECK, asHtml(wrapCentered("Slow<br>Spans")),
        false
    )

    val panelOfList = panel {
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

    val result = JBPanel<JBPanel<*>>()
    result.layout = BoxLayout(result, BoxLayout.Y_AXIS)
    result.add(topContents)
    result.add(Box.createVerticalStrut(5))
    result.add(panelOfList)

    return insightItemPanel(result)
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

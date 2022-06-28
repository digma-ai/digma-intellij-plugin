package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.Percentile
import org.digma.intellij.plugin.model.rest.insights.SlowSpanInfo
import org.digma.intellij.plugin.model.rest.insights.SlowestSpansInsight
import org.digma.intellij.plugin.service.InsightsActionsService
import org.digma.intellij.plugin.ui.common.*
import java.awt.BorderLayout
import java.awt.GridLayout
import java.math.BigDecimal
import java.math.RoundingMode
import javax.swing.*

fun slowestSpansPanel(project: Project, insight: SlowestSpansInsight, moreData: HashMap<String, Any>): JPanel {


    val title = JLabel(asHtml("${htmlSpanTitle()}<b>Span Bottleneck</b><br> " +
                                        "${htmlSpanSmoked()}The following spans are slowing request handling"),
                                        SwingConstants.LEFT)


    val spansListPanel = JPanel()
    spansListPanel.layout = GridLayout(insight.spans.size, 1, 0, 3)
    spansListPanel.border = JBUI.Borders.empty()
    insight.spans.forEach { slowSpan: SlowSpanInfo ->

        val displayName = slowSpan.spanInfo.displayName
        val spanId = CodeObjectsUtil.createSpanId(slowSpan.spanInfo.instrumentationLibrary, slowSpan.spanInfo.name)
        val spanText = asHtml("<b>${displayName}</b><br>${htmlSpanSmoked()}${descriptionOf(slowSpan)}")
        if (moreData.contains(spanId)) {
            val link = ActionLink(spanText) {
                val actionListener: InsightsActionsService = project.getService(InsightsActionsService::class.java)
                val workspaceUri: Pair<String, Int> = moreData[spanId] as Pair<String, Int>
                actionListener.openWorkspaceFileForSpan(workspaceUri.first, workspaceUri.second)
            }
            link.toolTipText = genToolTip(slowSpan)
            spansListPanel.add(link)
        } else {
            val label = JBLabel(spanText)
            label.toolTipText = genToolTip(slowSpan)
            spansListPanel.add(label)
        }
    }


    val iconPanel = panel {
        row {
            cell(iconPanelBorder(Icons.Insight.BOTTLENECK, asHtml(wrapCentered("Slow<br>Spans)"))))
                .horizontalAlign(HorizontalAlign.RIGHT)
        }.layout(RowLayout.INDEPENDENT)
    }
    iconPanel.border = JBUI.Borders.empty()


    val spansWrapper = JBPanel<JBPanel<*>>()
    spansWrapper.layout = BorderLayout(0,10)
    spansWrapper.add(title, BorderLayout.NORTH)
    spansWrapper.add(spansListPanel, BorderLayout.CENTER)
    spansWrapper.border = BorderFactory.createEmptyBorder()


    val iconPanelWrapper = JBPanel<JBPanel<*>>()
    iconPanelWrapper.layout = BorderLayout()
    iconPanelWrapper.add(iconPanel, BorderLayout.EAST)


    val result = JBPanel<JBPanel<*>>()
    result.layout = BoxLayout(result, BoxLayout.X_AXIS)
    result.add(spansWrapper)
    result.add(Box.createHorizontalStrut(5))
    result.add(iconPanelWrapper)

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

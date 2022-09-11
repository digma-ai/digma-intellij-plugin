package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.model.rest.insights.Percentile
import org.digma.intellij.plugin.model.rest.insights.SlowSpanInfo
import org.digma.intellij.plugin.model.rest.insights.SlowestSpansInsight
import org.digma.intellij.plugin.model.rest.insights.SpanSlowEndpointsInsight
import org.digma.intellij.plugin.service.InsightsActionsService
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.buildBoldTitleGrayedComment
import org.digma.intellij.plugin.ui.common.buildLinkTextWithTitleAndGrayedComment
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import java.awt.GridLayout
import java.math.BigDecimal
import java.math.RoundingMode
import javax.swing.JPanel

fun slowestSpansPanel(project: Project, insight: SlowestSpansInsight, moreData: HashMap<String, Any>, panelsLayoutHelper: PanelsLayoutHelper): JPanel {

    val spansListPanel = JPanel()
    spansListPanel.layout = GridLayout(insight.spans.size, 1, 0, 3)
    spansListPanel.border = JBUI.Borders.empty()
    spansListPanel.isOpaque = false
    insight.spans.forEach { slowSpan: SlowSpanInfo ->

        val displayName = slowSpan.spanInfo.displayName
        val description = descriptionOf(slowSpan)
        val spanId = CodeObjectsUtil.createSpanId(slowSpan.spanInfo.instrumentationLibrary, slowSpan.spanInfo.name)

        if (moreData.contains(spanId)) {
            val spanText = buildLinkTextWithTitleAndGrayedComment(displayName,description)
            val link = ActionLink(spanText) {
                val actionListener: InsightsActionsService = project.getService(InsightsActionsService::class.java)
                @Suppress("UNCHECKED_CAST")
                val workspaceUri: Pair<String, Int> = moreData[spanId] as Pair<String, Int>
                actionListener.openWorkspaceFileForSpan(workspaceUri.first, workspaceUri.second)
            }
            link.toolTipText = genToolTip(slowSpan)
            spansListPanel.add(link)
        } else {
            val spanText = buildBoldTitleGrayedComment(displayName,description)
            val label = JBLabel(spanText)
            label.toolTipText = genToolTip(slowSpan)
            spansListPanel.add(label)
        }
    }

    return createInsightPanel(
            "Span Bottleneck",
            "The following spans are slowing request handling",
            Laf.Icons.Insight.BOTTLENECK,
            spansListPanel,
            null,
            panelsLayoutHelper)
}

fun spanSlowEndpointsPanel(project: Project, insight: SpanSlowEndpointsInsight, panelsLayoutHelper: PanelsLayoutHelper): JPanel {
    val endpointsListPanel = JPanel()
    //TODO: fill data from insight itself

    return createInsightPanel(
        "Bottleneck",
        "The following trace sources spend a significant portion here:",
        Laf.Icons.Insight.BOTTLENECK,
        endpointsListPanel,
        null,
        panelsLayoutHelper)
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

package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI.Borders.empty
import org.apache.commons.lang3.StringUtils
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator
import org.digma.intellij.plugin.model.rest.insights.EndpointSchema
import org.digma.intellij.plugin.model.rest.insights.Percentile
import org.digma.intellij.plugin.model.rest.insights.SlowEndpointInfo
import org.digma.intellij.plugin.model.rest.insights.SlowSpanInfo
import org.digma.intellij.plugin.model.rest.insights.SlowestSpansInsight
import org.digma.intellij.plugin.model.rest.insights.SpanSlowEndpointsInsight
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.spanGrayed
import java.awt.BorderLayout
import java.awt.GridLayout
import java.math.BigDecimal
import java.math.RoundingMode
import javax.swing.JPanel
import javax.swing.SwingConstants

fun slowestSpansPanel(project: Project, insight: SlowestSpansInsight): JPanel {

    val spansListPanel = createDefaultBoxLayoutYAxisPanel()

    insight.spans.forEach { slowSpan: SlowSpanInfo ->

        val displayName = slowSpan.spanInfo.displayName
        val description = descriptionOf(slowSpan)
        val spanId = slowSpan.spanInfo.spanCodeObjectId

        val normalizedDisplayName = StringUtils.normalizeSpace(displayName)
        val grayedDescription = asHtml(spanGrayed(description))
        val descriptionLabel = JBLabel(grayedDescription, SwingConstants.LEFT)
        val link = ActionLink(normalizedDisplayName) {
            project.service<InsightsViewOrchestrator>().showInsightsForCodelessSpan(spanId)
        }
        link.toolTipText = genToolTip(slowSpan)

        val spanOneRecordPanel = getDefaultSpanOneRecordPanel()
        spanOneRecordPanel.add(link, BorderLayout.NORTH)
        spanOneRecordPanel.add(descriptionLabel, BorderLayout.SOUTH)
        spansListPanel.add(spanOneRecordPanel)

    }

    return createInsightPanel(
        project = project,
        insight = insight,
        title = "Span Bottleneck",
        description = "The following spans are slowing request handling",
        iconsList = listOf(Laf.Icons.Insight.BOTTLENECK),
        bodyPanel = spansListPanel,
        buttons = null,
        paginationComponent = null
    )
}

fun spanSlowEndpointsPanel(project: Project, insight: SpanSlowEndpointsInsight): JPanel {
    val endpointsListPanel = JPanel()
    endpointsListPanel.layout = GridLayout(insight.slowEndpoints.size, 1, 0, 3)
    endpointsListPanel.border = empty()
    endpointsListPanel.isOpaque = false

    insight.slowEndpoints.forEach { slowEndpointInfo ->
        val currContainerPanel = JPanel(GridLayout(2, 1, 0, 3))
        endpointsListPanel.border = empty()
        currContainerPanel.isOpaque = false

        val routeInfo = EndpointSchema.getRouteInfo(slowEndpointInfo.endpointInfo.route)
        val shortRouteName = routeInfo.shortName

        val normalizedDisplayName = StringUtils.normalizeSpace(shortRouteName)
        val link = ActionLink(normalizedDisplayName) {
            project.service<InsightsViewOrchestrator>().showInsightsForCodelessSpan(slowEndpointInfo.endpointInfo.spanCodeObjectId)
        }
        link.toolTipText = asHtml(shortRouteName)
        currContainerPanel.add(link, BorderLayout.NORTH)


        val line2 = JBLabel(asHtml(descriptionOf(slowEndpointInfo)))
        currContainerPanel.add(line2)
        endpointsListPanel.add(currContainerPanel)
    }

    return createInsightPanel(
        project = project,
        insight = insight,
        title = "Bottleneck",
        description = "The following trace sources spend a significant portion here:",
        iconsList = listOf(Laf.Icons.Insight.BOTTLENECK),
        bodyPanel = endpointsListPanel,
        buttons = null,
        paginationComponent = null
    )
}

fun descriptionOf(sei: SlowEndpointInfo): String {
    if (sei.ProbabilityOfBeingBottleneck != null &&
        sei.AvgDurationWhenBeingBottleneck != null
    ) {

        return "Slowing ${(sei.ProbabilityOfBeingBottleneck!! * 100).toInt()}% of the requests (~${sei.AvgDurationWhenBeingBottleneck!!.value}${sei.AvgDurationWhenBeingBottleneck!!.unit})"

    } else { // Obsolete

        if (sei.p95.fraction > 0) {
            return "Up to ~${percentageForDisplaySlowSpanInfo(sei.p95)} of entire of the entire request time ${durationText(sei.p95)}"
        }

        return "Up to ~${percentageForDisplaySlowSpanInfo(sei.p50)} of entire of the entire request time ${durationText(sei.p50)}"
    }
}

fun durationText(percentile: Percentile): String {
    return "${percentile.maxDuration.value}${percentile.maxDuration.unit}"
}

fun descriptionOf(span: SlowSpanInfo): String {
    if (span.probabilityOfBeingBottleneck != null &&
        span.avgDurationWhenBeingBottleneck != null
    ) {

        return "Slowing ${(span.probabilityOfBeingBottleneck!! * 100).toInt()}% of the requests (~${span.avgDurationWhenBeingBottleneck!!.value}${span.avgDurationWhenBeingBottleneck!!.unit})"

    } else { // Obsolete

        if (span.p50.fraction > 0.4)
            return "50% of the users by up to ${span.p50.maxDuration.value}${span.p50.maxDuration.unit}"

        if (span.p95.fraction > 0.4)
            return "5% of the users by up to ${span.p95.maxDuration.value}${span.p95.maxDuration.unit}"

        return "1% of the users by up to ${span.p99.maxDuration.value}${span.p99.maxDuration.unit}"
    }
}

private fun percentageForDisplaySlowSpanInfo(percentile: Percentile): String {
    val decimal = BigDecimal(percentile.fraction * 100).setScale(3, RoundingMode.HALF_DOWN)
    return decimal.toPlainString()
}

fun genToolTip(span: SlowSpanInfo): String {
    return asHtml(span.spanInfo.displayName)
//        """
//Percentage of time spent in span:
//<pre>
//Median: ${oneLiner(span.p50)}%
//P95:    ${oneLiner(span.p95)}%
//P99:    ${oneLiner(span.p99)}%
//</pre>
//"""
//    )
}

//private fun percentageForDisplay(percentile: Percentile): String {
//    val decimal = BigDecimal(percentile.fraction * 100).setScale(0, RoundingMode.HALF_DOWN)
//    return decimal.toPlainString()
//}

//private fun oneLiner(percentile: Percentile): String {
//    return "${percentageForDisplay(percentile)}% ~${percentile.maxDuration.value}${percentile.maxDuration.unit}"
//}

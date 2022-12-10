package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.model.rest.insights.*
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.buildBoldTitleGrayedComment
import org.digma.intellij.plugin.ui.common.buildLinkTextWithTitleAndGrayedComment
import org.digma.intellij.plugin.ui.list.openWorkspaceFileForSpan
import java.awt.GridLayout
import java.math.BigDecimal
import java.math.RoundingMode
import javax.swing.JPanel

fun slowestSpansPanel(project: Project, insight: SlowestSpansInsight, moreData: HashMap<String, Any>): JPanel {

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
                openWorkspaceFileForSpan(project, moreData, spanId)
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
    endpointsListPanel.border = JBUI.Borders.empty()
    endpointsListPanel.isOpaque = false

    insight.slowEndpoints.forEach {slowEndpointInfo ->
        val currContainerPanel = JPanel(GridLayout(2, 1, 0, 3))
        endpointsListPanel.border = JBUI.Borders.empty()
        currContainerPanel.isOpaque = false

        val routeInfo = EndpointSchema.getRouteInfo(slowEndpointInfo.endpointInfo.route)
        val shortRouteName =  routeInfo.shortName

        val line1 = JBLabel(asHtml("${slowEndpointInfo.endpointInfo.serviceName}: <b>$shortRouteName</b>"))
        val line2 = JBLabel(asHtml(descriptionOf(slowEndpointInfo)))

        currContainerPanel.add(line1)
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
        sei.AvgDurationWhenBeingBottleneck != null){

        return "Slowing ${(sei.ProbabilityOfBeingBottleneck!!*100).toInt()}% of the requests (~${sei.AvgDurationWhenBeingBottleneck!!.value}${sei.AvgDurationWhenBeingBottleneck!!.unit})"

    }
    else{ // Obsolete

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
    if (span.ProbabilityOfBeingBottleneck != null &&
        span.AvgDurationWhenBeingBottleneck != null){

        return "Slowing ${(span.ProbabilityOfBeingBottleneck!!*100).toInt()}% of the requests (~${span.AvgDurationWhenBeingBottleneck!!.value}${span.AvgDurationWhenBeingBottleneck!!.unit})"

    }
    else{ // Obsolete

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

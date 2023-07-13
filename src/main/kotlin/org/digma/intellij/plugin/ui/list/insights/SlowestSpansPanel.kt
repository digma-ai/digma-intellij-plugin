package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI.Borders.empty
import org.apache.commons.lang3.StringUtils
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.rest.insights.EndpointSchema
import org.digma.intellij.plugin.model.rest.insights.SlowSpanInfo
import org.digma.intellij.plugin.model.rest.insights.SlowestSpansInsight
import org.digma.intellij.plugin.model.rest.insights.SpanSlowEndpointsInsight
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.spanGrayed
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.JPanel
import javax.swing.SwingConstants

fun slowestSpansPanel(project: Project, insight: SlowestSpansInsight): JPanel {

    val spansListPanel = createDefaultBoxLayoutYAxisPanel()

    insight.spans.forEach { slowSpan: SlowSpanInfo ->

        val displayName = slowSpan.spanInfo.displayName
        val description = "Slowing ${(slowSpan.probabilityOfBeingBottleneck*100).toInt()}% of the requests (~${slowSpan.avgDurationWhenBeingBottleneck.value}${slowSpan.avgDurationWhenBeingBottleneck.unit})"
        val spanId = slowSpan.spanInfo.spanCodeObjectId

        val normalizedDisplayName = StringUtils.normalizeSpace(displayName)
        val grayedDescription = asHtml(spanGrayed(description))
        val descriptionLabel = JBLabel(grayedDescription, SwingConstants.LEFT)
        val link = ActionLink(normalizedDisplayName) {
            ActivityMonitor.getInstance(project).registerSpanLinkClicked(InsightType.SlowestSpans)
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
            ActivityMonitor.getInstance(project).registerSpanLinkClicked(InsightType.SlowestSpans)
            project.service<InsightsViewOrchestrator>().showInsightsForCodelessSpan(slowEndpointInfo.endpointInfo.spanCodeObjectId)
        }
        link.toolTipText = asHtml(shortRouteName)
        currContainerPanel.add(link, BorderLayout.NORTH)


        val line2 = JBLabel(asHtml("Slowing ${(slowEndpointInfo.ProbabilityOfBeingBottleneck*100).toInt()}% of the requests (~${slowEndpointInfo.AvgDurationWhenBeingBottleneck.value}${slowEndpointInfo.AvgDurationWhenBeingBottleneck.unit})"))
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

fun genToolTip(span: SlowSpanInfo): String {
    return asHtml(span.spanInfo.displayName)
}

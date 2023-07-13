package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import org.apache.commons.lang3.StringUtils
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.rest.insights.EndpointSchema
import org.digma.intellij.plugin.model.rest.insights.SpanNPlusEndpoints
import org.digma.intellij.plugin.model.rest.insights.SpanNPlusOneInsight
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.spanBold
import org.digma.intellij.plugin.ui.common.spanGrayed
import org.digma.intellij.plugin.ui.model.TraceSample
import java.awt.BorderLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

fun spanNPlusOneInsightPanel(project: Project, insight: SpanNPlusOneInsight): JPanel {
    val resultPanel = JPanel()
    resultPanel.layout = BoxLayout(resultPanel, BoxLayout.Y_AXIS)
    resultPanel.isOpaque = false

    resultPanel.add(getMainDescriptionPanel(project, insight))
    resultPanel.add(getRowPanel(insight))
    resultPanel.add(getAffectedEndpointTitle())

    for (endpoint in insight.endpoints) {
        resultPanel.add(getAffectedEndpointRow(project, endpoint))
    }

    return createInsightPanel(
        project = project,
        insight = insight,
        title = "Suspected N-Plus-1",
        description = asHtml("Check the following SELECT statement"),
        iconsList = listOf(Laf.Icons.Insight.N_PLUS_ONE),
        bodyPanel = resultPanel,
        buttons = listOf(getButtonToJaeger(project, insight)),
        paginationComponent = null
    )
}

private fun getAffectedEndpointTitle(): JPanel {
    val labelPanel = JPanel(BorderLayout())

    labelPanel.border = JBUI.Borders.emptyBottom(5)
    labelPanel.isOpaque = false

    val label = JLabel(asHtml(spanGrayed("Affected Endpoints:")), SwingConstants.LEFT)

    label.isOpaque = false
    labelPanel.add(label)

    return labelPanel
}

private fun getAffectedEndpointRow(project: Project, endpoint: SpanNPlusEndpoints): JPanel {
    val endpointPanel = JPanel(BorderLayout())
    endpointPanel.border = JBUI.Borders.emptyBottom(5)
    endpointPanel.isOpaque = false

    val routeInfo = EndpointSchema.getRouteInfo(endpoint.endpointInfo.route)
    val shortRouteName = routeInfo.shortName

    val normalizedDisplayName = StringUtils.normalizeSpace(shortRouteName)
    if (endpoint.endpointInfo.entrySpanCodeObjectId != null) {
        val link = ActionLink(normalizedDisplayName) {
            ActivityMonitor.getInstance(project).registerSpanLinkClicked(InsightType.SpaNPlusOne)
            project.service<InsightsViewOrchestrator>().showInsightsForCodelessSpan(endpoint.endpointInfo.entrySpanCodeObjectId!!)
        }
        link.toolTipText = asHtml(shortRouteName)
        endpointPanel.add(link, BorderLayout.NORTH)
    } else {
        val jbLabel = JBLabel(normalizedDisplayName, SwingConstants.TRAILING)
        jbLabel.toolTipText = asHtml(shortRouteName)
        jbLabel.horizontalAlignment = SwingConstants.LEFT
        endpointPanel.add(jbLabel, BorderLayout.NORTH)
    }


    val repeatsValue = "${endpoint.occurrences} (median)"
    val repeatsLabel = JLabel(asHtml("Repeats: ${spanBold(repeatsValue)}"))
    endpointPanel.add(repeatsLabel, BorderLayout.WEST)

    return endpointPanel
}


private fun getButtonToJaeger(project: Project, insight: SpanNPlusOneInsight): JButton? {
    val spanName = insight.clientSpanName
    val sampleTraceId = insight.traceId
    val traceSample = spanName?.let { TraceSample(it, sampleTraceId) }
    return spanName?.let { buildButtonToJaeger(project, "Trace", it, listOf(traceSample), InsightType.SpaNPlusOne) }
}

private fun getMainDescriptionPanel(project: Project, insight: SpanNPlusOneInsight): JPanel {
    val spanOneRecordPanel = getDefaultSpanOneRecordPanel()

    val displayText: String? = insight.clientSpanName
    if (StringUtils.isEmpty(displayText))
        return spanOneRecordPanel

    val normalizedDisplayText = StringUtils.normalizeSpace(insight.clientSpanName)
    if (insight.clientSpanCodeObjectId != null) {
        val actionLink = ActionLink(normalizedDisplayText) {
            ActivityMonitor.getInstance(project).registerSpanLinkClicked(InsightType.SpaNPlusOne)
            project.service<InsightsViewOrchestrator>().showInsightsForCodelessSpan(insight.clientSpanCodeObjectId!!)
        }
        actionLink.toolTipText = asHtml(displayText)
        actionLink.horizontalAlignment = SwingConstants.LEFT
        spanOneRecordPanel.add(actionLink, BorderLayout.NORTH)
    } else {
        val jbLabel = JBLabel(normalizedDisplayText, SwingConstants.TRAILING)
        jbLabel.toolTipText = asHtml(displayText)
        jbLabel.horizontalAlignment = SwingConstants.LEFT
        spanOneRecordPanel.add(jbLabel, BorderLayout.NORTH)
    }

    return spanOneRecordPanel
}

private fun getRowPanel(insight: SpanNPlusOneInsight): JPanel {
    val rowPanel = createDefaultBoxLayoutLineAxisPanel()
    rowPanel.border = JBUI.Borders.emptyBottom(10)

    val repeatsValue = "${insight.occurrences} (median)"
    val repeatsLabel = JLabel(asHtml("Repeats: ${spanBold(repeatsValue)}"))
    val durationLabel = JLabel(
        asHtml(
            "Duration: " +
                    spanBold("${insight.duration.value} ${insight.duration.unit}")
        )
    )

    rowPanel.add(repeatsLabel)
    rowPanel.add(Box.createHorizontalGlue())
    rowPanel.add(durationLabel)
    return rowPanel
}
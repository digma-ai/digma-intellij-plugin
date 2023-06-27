package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import org.apache.commons.lang3.StringUtils
import org.digma.intellij.plugin.editor.getCurrentPageNumberForInsight
import org.digma.intellij.plugin.editor.updateListOfEntriesToDisplay
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.rest.insights.EndpointSessionInViewInsight
import org.digma.intellij.plugin.model.rest.insights.SessionInViewSpanInfo
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.model.TraceSample
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants

private const val RECORDS_PER_PAGE_SIV = 3

fun sessionInViewPanel(
    project: Project,
    insight: EndpointSessionInViewInsight
): JPanel {

    val uniqueInsightId = insight.codeObjectId + insight.type
    val lastPageNum: Int
    var resultSessionInViewPanel: DigmaResettablePanel? = null
    val paginationPanel = JPanel()
    val sessionInViewSpansToDisplay = ArrayList<SessionInViewSpanInfo>()
    val spansOfInsight = insight.spans

    lastPageNum = countNumberOfPages(spansOfInsight.size, RECORDS_PER_PAGE_SIV)

    resultSessionInViewPanel = object : DigmaResettablePanel() {
        override fun reset() {
            rebuildSIVInsightRowsPanel(
                resultSessionInViewPanel!!,
                sessionInViewSpansToDisplay,
                project
            )
            rebuildPaginationPanel(paginationPanel, lastPageNum,
                spansOfInsight, resultSessionInViewPanel, sessionInViewSpansToDisplay, uniqueInsightId, RECORDS_PER_PAGE_SIV, project, insight.type)
        }
    }

    updateListOfEntriesToDisplay(spansOfInsight, sessionInViewSpansToDisplay, getCurrentPageNumberForInsight(uniqueInsightId, lastPageNum), RECORDS_PER_PAGE_SIV, project)
    rebuildSIVInsightRowsPanel(resultSessionInViewPanel, sessionInViewSpansToDisplay, project)

    val result = createInsightPanel(
        project = project,
        insight = insight,
        title = "Session in View Query Detected",
        description = asHtml("Query execution was detected during the view rendering."),
        iconsList = listOf(Laf.Icons.Insight.N_PLUS_ONE),
        bodyPanel = resultSessionInViewPanel,
        buttons = listOf(getButtonToJaeger(project, insight)),
        paginationComponent = buildPaginationRowPanel(lastPageNum, paginationPanel,
            spansOfInsight, resultSessionInViewPanel, sessionInViewSpansToDisplay, uniqueInsightId, RECORDS_PER_PAGE_SIV, project, insight.type),
    )
    result.toolTipText = asHtml("Queries detected in rendering span suggestion a Session in View anti-pattern")
    return result
}

private fun rebuildSIVInsightRowsPanel(
    sessionInViewPanel: DigmaResettablePanel,
    sessionInViewSpansToDisplay: List<SessionInViewSpanInfo>,
    project: Project) {
    sessionInViewPanel.removeAll()
    buildSIVInsightRowsPanel(sessionInViewPanel, sessionInViewSpansToDisplay, project)
}

private fun buildSIVInsightRowsPanel(
    nPOnePanel: DigmaResettablePanel,
    sivSpansToDisplay: List<SessionInViewSpanInfo>,
    project: Project
) {
    nPOnePanel.layout = BoxLayout(nPOnePanel, BoxLayout.Y_AXIS)
    nPOnePanel.isOpaque = false

    sivSpansToDisplay.forEach { sivSpanInfo: SessionInViewSpanInfo ->
        nPOnePanel.add(sivSpanRowPanel(sivSpanInfo, project))
    }
}

private fun sivSpanRowPanel(span: SessionInViewSpanInfo, project: Project): JPanel {
    val resultPanel = createDefaultBoxLayoutYAxisPanel()
    resultPanel.add(getMainDescriptionPanel(span, project))
    return resultPanel
}

private fun getButtonToJaeger(project: Project, insight: EndpointSessionInViewInsight): JButton? {
    val spanName = insight.endpointSpanName()
    val sampleTraceId = insight.spans.first().traceId
    val traceSample = TraceSample(spanName, sampleTraceId)
    return buildButtonToJaeger(project, "Trace", spanName, listOf(traceSample), InsightType.EndpointSpaNPlusOne)
}

private fun getMainDescriptionPanel(span: SessionInViewSpanInfo, project: Project): JPanel {
    val spanOneRecordPanel = getDefaultSpanOneRecordPanel()
    val displayText: String?
    if (span.renderSpan != null) {
        val spanId = span.renderSpan!!.spanCodeObjectId
        displayText = span.renderSpan?.displayName
        addMainDescriptionLabelWithLink(spanOneRecordPanel, displayText, spanId, project)
    } else {
        displayText = span.clientSpan?.displayName
        if (StringUtils.isNotEmpty(displayText)) {
            val normalizedDisplayName = StringUtils.normalizeSpace(displayText)
            val jbLabel = JBLabel(normalizedDisplayName, SwingConstants.TRAILING)
            jbLabel.toolTipText = asHtml(displayText)
            jbLabel.horizontalAlignment = SwingConstants.LEFT
            spanOneRecordPanel.add(jbLabel, BorderLayout.NORTH)
        }
    }
    return spanOneRecordPanel
}

private fun addMainDescriptionLabelWithLink(spanOneRecordPanel: JPanel, displayText: String?, spanId: String, project: Project) {
    if ( StringUtils.isNotEmpty(displayText)) {
        val normalizedDisplayName = StringUtils.normalizeSpace(displayText)
        val actionLink = ActionLink(normalizedDisplayName) {
            project.service<InsightsViewOrchestrator>().showInsightsForCodelessSpan(spanId)
        }
        actionLink.toolTipText = asHtml(displayText)
        actionLink.horizontalAlignment = SwingConstants.LEFT
        spanOneRecordPanel.add(actionLink, BorderLayout.NORTH)
    }
}


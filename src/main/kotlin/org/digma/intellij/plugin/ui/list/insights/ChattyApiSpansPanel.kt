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
import org.digma.intellij.plugin.model.rest.insights.ChattyApiSpanInfo
import org.digma.intellij.plugin.model.rest.insights.EndpointChattyApiInsight
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

private const val RECORDS_PER_PAGE_CHATTY_API = 3

fun chattyApiPanel(
    project: Project,
    insight: EndpointChattyApiInsight
): JPanel {

    val uniqueInsightId = insight.codeObjectId + insight.type
    val lastPageNum: Int
    var resultChattyApiPanel: DigmaResettablePanel? = null
    val paginationPanel = JPanel()
    val sessionInViewSpansToDisplay = ArrayList<ChattyApiSpanInfo>()
    val spansOfInsight = insight.spans

    lastPageNum = countNumberOfPages(spansOfInsight.size, RECORDS_PER_PAGE_CHATTY_API)

    resultChattyApiPanel = object : DigmaResettablePanel() {
        override fun reset() {
            rebuildChattyApiInsightRowsPanel(
                resultChattyApiPanel!!,
                sessionInViewSpansToDisplay,
                project
            )
            rebuildPaginationPanel(paginationPanel, lastPageNum,
                spansOfInsight, resultChattyApiPanel, sessionInViewSpansToDisplay, uniqueInsightId, RECORDS_PER_PAGE_CHATTY_API,project, insight.type)
        }
    }

    updateListOfEntriesToDisplay(spansOfInsight, sessionInViewSpansToDisplay, getCurrentPageNumberForInsight(uniqueInsightId, lastPageNum), RECORDS_PER_PAGE_CHATTY_API, project)
    rebuildChattyApiInsightRowsPanel(resultChattyApiPanel, sessionInViewSpansToDisplay, project)

    val result = createInsightPanel(
        project = project,
        insight = insight,
        title = "Excessive API calls detected",
        description = asHtml("Excessive API calls to specific endpoint found"),
        iconsList = listOf(Laf.Icons.Insight.N_PLUS_ONE),
        bodyPanel = resultChattyApiPanel,
        buttons = listOf(getButtonToJaeger(project, insight)),
        paginationComponent = buildPaginationRowPanel(lastPageNum, paginationPanel,
            spansOfInsight, resultChattyApiPanel, sessionInViewSpansToDisplay, uniqueInsightId, RECORDS_PER_PAGE_CHATTY_API, project, insight.type),
    )
    result.toolTipText = asHtml("Many HTTP calls were detected in the trace, consider using a different API or caching responses to reduce chatter.")
    return result
}

private fun rebuildChattyApiInsightRowsPanel(
    sessionInViewPanel: DigmaResettablePanel,
    chattApiSpansToDisplay: List<ChattyApiSpanInfo>,
    project: Project) {
    sessionInViewPanel.removeAll()
    buildChattyApiRowPanel(sessionInViewPanel, chattApiSpansToDisplay, project)
}

private fun buildChattyApiRowPanel(
    nPOnePanel: DigmaResettablePanel,
    sivSpansToDisplay: List<ChattyApiSpanInfo>,
    project: Project
) {
    nPOnePanel.layout = BoxLayout(nPOnePanel, BoxLayout.Y_AXIS)
    nPOnePanel.isOpaque = false

    sivSpansToDisplay.forEach { sivSpanInfo: ChattyApiSpanInfo ->
        nPOnePanel.add(sivSpanRowPanel(sivSpanInfo, project))
    }
}

private fun sivSpanRowPanel(span: ChattyApiSpanInfo, project: Project): JPanel {
    val resultPanel = createDefaultBoxLayoutYAxisPanel()
    resultPanel.add(getMainDescriptionPanel(span, project))
    return resultPanel
}
private fun getButtonToJaeger(project: Project, insight: EndpointChattyApiInsight): JButton? {
    val spanName = insight.endpointSpanName()
    val sampleTraceId = insight.spans.first().traceId
    val traceSample = TraceSample(spanName, sampleTraceId)
    return buildButtonToJaeger(project, "Trace", spanName, listOf(traceSample), InsightType.EndpointSpaNPlusOne)
}

private fun getMainDescriptionPanel(span: ChattyApiSpanInfo, project: Project): JPanel {
    val spanOneRecordPanel = getDefaultSpanOneRecordPanel()
    val displayText: String?
    if (span.clientSpan != null) {
        val spanId = span.clientSpan!!.spanCodeObjectId
        displayText = span.clientSpan?.displayName
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


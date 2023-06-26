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
import org.digma.intellij.plugin.model.rest.insights.EPNPlusSpansInsight
import org.digma.intellij.plugin.model.rest.insights.EndpointModelInViewInsight
import org.digma.intellij.plugin.model.rest.insights.HighlyOccurringSpanInfo
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.spanBold
import org.digma.intellij.plugin.ui.model.TraceSample
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.BorderLayout
import java.math.RoundingMode
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

private const val RECORDS_PER_PAGE_EPNPLUS = 3

fun mivPanel(
    project: Project,
    insight: EndpointModelInViewInsight
): JPanel {

    val uniqueInsightId = insight.codeObjectId + insight.type
    val lastPageNum: Int
    var resultNPOnePanel: DigmaResettablePanel? = null
    val paginationPanel = JPanel()
    val nPOneSpansToDisplay = ArrayList<HighlyOccurringSpanInfo>()
    val spansOfInsight = insight.spans

    lastPageNum = countNumberOfPages(spansOfInsight.size, RECORDS_PER_PAGE_EPNPLUS)

    resultNPOnePanel = object : DigmaResettablePanel() {
        override fun reset() {
            rebuildENPlusInsightRowsPanel(
                resultNPOnePanel!!,
                nPOneSpansToDisplay,
                project
            )
            rebuildPaginationPanel(paginationPanel, lastPageNum,
                spansOfInsight, resultNPOnePanel, nPOneSpansToDisplay, uniqueInsightId, RECORDS_PER_PAGE_EPNPLUS, project, insight.type)
        }
    }

    updateListOfEntriesToDisplay(spansOfInsight, nPOneSpansToDisplay, getCurrentPageNumberForInsight(uniqueInsightId, lastPageNum), RECORDS_PER_PAGE_EPNPLUS, project)
    buildENPlusInsightRowsPanel(resultNPOnePanel, nPOneSpansToDisplay, project)

    val result = createInsightPanel(
        project = project,
        insight = insight,
        title = "Model in View Query Detected",
        description = asHtml("Query execution was detected during the view rendering."),
        iconsList = listOf(Laf.Icons.Insight.N_PLUS_ONE),
        bodyPanel = resultNPOnePanel,
        buttons = listOf(getButtonToJaeger(project, insight)),
        paginationComponent = buildPaginationRowPanel(lastPageNum, paginationPanel,
            spansOfInsight, resultNPOnePanel, nPOneSpansToDisplay, uniqueInsightId, RECORDS_PER_PAGE_EPNPLUS, project, insight.type),
    )
    result.toolTipText = asHtml("Repeating select query pattern suggests N-Plus-One")
    return result
}

fun ePNPlusSpansPanel(
        project: Project,
        insight: EPNPlusSpansInsight
): JPanel {

    val uniqueInsightId = insight.codeObjectId + insight.type
    val lastPageNum: Int
    var resultNPOnePanel: DigmaResettablePanel? = null
    val paginationPanel = JPanel()
    val nPOneSpansToDisplay = ArrayList<HighlyOccurringSpanInfo>()
    val spansOfInsight = insight.spans

    lastPageNum = countNumberOfPages(spansOfInsight.size, RECORDS_PER_PAGE_EPNPLUS)

    resultNPOnePanel = object : DigmaResettablePanel() {
        override fun reset() {
            rebuildENPlusInsightRowsPanel(
                    resultNPOnePanel!!,
                    nPOneSpansToDisplay,
                    project
            )
            rebuildPaginationPanel(paginationPanel, lastPageNum,
                    spansOfInsight, resultNPOnePanel, nPOneSpansToDisplay, uniqueInsightId, RECORDS_PER_PAGE_EPNPLUS, project, insight.type)
        }
    }

    updateListOfEntriesToDisplay(spansOfInsight, nPOneSpansToDisplay, getCurrentPageNumberForInsight(uniqueInsightId, lastPageNum), RECORDS_PER_PAGE_EPNPLUS, project)
    buildENPlusInsightRowsPanel(resultNPOnePanel, nPOneSpansToDisplay, project)

    val result = createInsightPanel(
            project = project,
            insight = insight,
            title = "Suspected N-Plus-1",
            description = asHtml("Check the following locations"),
            iconsList = listOf(Laf.Icons.Insight.N_PLUS_ONE),
            bodyPanel = resultNPOnePanel,
            buttons = listOf(getButtonToJaeger(project, insight)),
            paginationComponent = buildPaginationRowPanel(lastPageNum, paginationPanel,
                    spansOfInsight, resultNPOnePanel, nPOneSpansToDisplay, uniqueInsightId, RECORDS_PER_PAGE_EPNPLUS, project, insight.type),
    )
    result.toolTipText = asHtml("Repeating select query pattern suggests N-Plus-One")
    return result
}

private fun getMainDescriptionPanel(span: HighlyOccurringSpanInfo, project: Project): JPanel {
    val spanOneRecordPanel = getDefaultSpanOneRecordPanel()
    val displayText: String?
    if (span.internalSpan != null) {
        val spanId = span.internalSpan!!.spanCodeObjectId
        displayText = span.internalSpan?.displayName
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

private fun getRowPanel(span: HighlyOccurringSpanInfo): JPanel {
    val rowPanel = createDefaultBoxLayoutLineAxisPanel()

    val repeatsValue = "${span.occurrences} (median)"
    val repeatsLabel = JLabel(asHtml("Repeats: ${spanBold(repeatsValue)}"))
    val impactLabel = getImpactLabel(span)
    val durationLabel = JLabel(asHtml("Duration: " +
            spanBold("${span.duration.value} ${span.duration.unit}")))

    rowPanel.add(repeatsLabel)
    rowPanel.add(Box.createHorizontalGlue())
    rowPanel.add(impactLabel)
    rowPanel.add(Box.createHorizontalGlue())
    rowPanel.add(durationLabel)
    return rowPanel
}

private fun getImpactLabel(span: HighlyOccurringSpanInfo): JLabel {
    val fraction = span.fraction
    val fractionSt = if (fraction < 0.01) {
        "minimal"
    } else {
        "${fraction.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()} of request"
    }
    return JLabel(asHtml("Impact: ${spanBold(fractionSt)}"))
}

private fun buildENPlusInsightRowsPanel(
        nPOnePanel: DigmaResettablePanel,
        nPOneSpansToDisplay: List<HighlyOccurringSpanInfo>,
        project: Project
) {
    nPOnePanel.layout = BoxLayout(nPOnePanel, BoxLayout.Y_AXIS)
    nPOnePanel.isOpaque = false

    nPOneSpansToDisplay.forEach { nPOneSpan: HighlyOccurringSpanInfo ->
        nPOnePanel.add(nPOneSpanRowPanel(nPOneSpan, project))
    }
}

private fun rebuildENPlusInsightRowsPanel(
        nPOnePanel: DigmaResettablePanel,
        nPOneSpansToDisplay: List<HighlyOccurringSpanInfo>,
        project: Project
) {
    nPOnePanel.removeAll()
    buildENPlusInsightRowsPanel(nPOnePanel, nPOneSpansToDisplay, project)
}

private fun nPOneSpanRowPanel(span: HighlyOccurringSpanInfo, project: Project): JPanel {
    val resultPanel = createDefaultBoxLayoutYAxisPanel()
    resultPanel.add(getMainDescriptionPanel(span, project))
    resultPanel.add(getRowPanel(span))
    return resultPanel
}

private fun getButtonToJaeger(project: Project, insight: EPNPlusSpansInsight): JButton? {
    val spanName = insight.endpointSpanName()
    val sampleTraceId = insight.spans.first().traceId
    val traceSample = TraceSample(spanName, sampleTraceId)
    return buildButtonToJaeger(project, "Trace", spanName, listOf(traceSample), InsightType.EndpointSpaNPlusOne)
}

private fun getButtonToJaeger(project: Project, insight: EndpointModelInViewInsight): JButton? {
    val spanName = insight.endpointSpanName()
    val sampleTraceId = insight.spans.first().traceId
    val traceSample = TraceSample(spanName, sampleTraceId)
    return buildButtonToJaeger(project, "Trace", spanName, listOf(traceSample), InsightType.EndpointSpaNPlusOne)
}
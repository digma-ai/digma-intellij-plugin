package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import org.apache.commons.lang3.StringUtils
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.editor.getCurrentPageNumberForInsight
import org.digma.intellij.plugin.editor.updateListOfEntriesToDisplay
import org.digma.intellij.plugin.model.rest.insights.EPNPlusSpansInsight
import org.digma.intellij.plugin.model.rest.insights.HighlyOccurringSpanInfo
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.spanBold
import org.digma.intellij.plugin.ui.list.openWorkspaceFileForSpan
import org.digma.intellij.plugin.ui.model.TraceSample
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.BorderLayout
import java.math.RoundingMode
import javax.swing.*

private const val RECORDS_PER_PAGE_EPNPLUS = 3

fun ePNPlusSpansPanel(
        project: Project,
        insight: EPNPlusSpansInsight,
        moreData: HashMap<String, Any>
): JPanel {

    val uniqueInsightId = insight.codeObjectId + insight.type
    val lastPageNum: Int
    var resultNPOnePanel: DigmaResettablePanel? = null
    val paginationPanel = JPanel()
    val nPOneSpansToDisplay = ArrayList<HighlyOccurringSpanInfo>()
    val spansOfInsight = insight.spans

    //calculate how many pages there are (display only 1 span per page)
    lastPageNum = spansOfInsight.size

    resultNPOnePanel = object : DigmaResettablePanel() {
        override fun reset() {
            rebuildENPlusInsightRowsPanel(
                    resultNPOnePanel!!,
                    nPOneSpansToDisplay,
                    project,
                    moreData
            )
            rebuildPaginationPanel(paginationPanel, lastPageNum,
                    spansOfInsight, resultNPOnePanel, nPOneSpansToDisplay, uniqueInsightId, RECORDS_PER_PAGE_EPNPLUS, project)
        }
    }

    updateListOfEntriesToDisplay(spansOfInsight, nPOneSpansToDisplay, getCurrentPageNumberForInsight(uniqueInsightId, lastPageNum), RECORDS_PER_PAGE_EPNPLUS, project)
    buildENPlusInsightRowsPanel(resultNPOnePanel, nPOneSpansToDisplay, project, moreData)

    val result = createInsightPanel(
            project = project,
            insight = insight,
            title = "Suspected N-Plus-1",
            description = asHtml("Check the following locations"),
            iconsList = listOf(Laf.Icons.Insight.N_PLUS_ONE),
            bodyPanel = resultNPOnePanel,
            buttons = listOf(getButtonToJaeger(project, insight)),
            paginationComponent = buildPaginationRowPanel(lastPageNum, paginationPanel,
                    spansOfInsight, resultNPOnePanel, nPOneSpansToDisplay, uniqueInsightId, RECORDS_PER_PAGE_EPNPLUS, project),
    )
    result.toolTipText = asHtml("Repeating select query pattern suggests N-Plus-One")
    return result
}

private fun getMainDescriptionPanel(span: HighlyOccurringSpanInfo, project: Project, moreData: HashMap<String, Any>): JPanel {
    val spanOneRecordPanel = getDefaultSpanOneRecordPanel()
    val displayText: String?
    if (span.internalSpan != null) {
        val spanId = CodeObjectsUtil.createSpanId(span.internalSpan!!.instrumentationLibrary, span.internalSpan!!.name)
        displayText = span.internalSpan?.displayName
        addMainDescriptionLabelWithLink(spanOneRecordPanel, displayText, spanId, project, moreData)
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

private fun addMainDescriptionLabelWithLink(spanOneRecordPanel: JPanel, displayText: String?, spanId: String, project: Project, moreData: HashMap<String, Any>) {
    if ( StringUtils.isNotEmpty(displayText)) {
        val normalizedDisplayName = StringUtils.normalizeSpace(displayText)
        if (moreData.contains(spanId)) {
            val actionLink = ActionLink(normalizedDisplayName) {
                openWorkspaceFileForSpan(project, moreData, spanId)
            }
            actionLink.toolTipText = asHtml(displayText)
            actionLink.horizontalAlignment = SwingConstants.LEFT
            spanOneRecordPanel.add(actionLink, BorderLayout.NORTH)
        } else {
            val jbLabel = JBLabel(displayText!!, SwingConstants.TRAILING)
            jbLabel.toolTipText = asHtml(displayText)
            jbLabel.horizontalAlignment = SwingConstants.LEFT
            spanOneRecordPanel.add(jbLabel, BorderLayout.NORTH)
        }
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
        project: Project,
        moreData: HashMap<String, Any>
) {
    nPOnePanel.layout = BoxLayout(nPOnePanel, BoxLayout.Y_AXIS)
    nPOnePanel.isOpaque = false

    nPOneSpansToDisplay.forEach { nPOneSpan: HighlyOccurringSpanInfo ->
        nPOnePanel.add(nPOneSpanRowPanel(nPOneSpan, project, moreData))
    }
}

private fun rebuildENPlusInsightRowsPanel(
        nPOnePanel: DigmaResettablePanel,
        nPOneSpansToDisplay: List<HighlyOccurringSpanInfo>,
        project: Project,
        moreData: HashMap<String, Any>
) {
    nPOnePanel.removeAll()
    buildENPlusInsightRowsPanel(nPOnePanel, nPOneSpansToDisplay, project, moreData)
}

private fun nPOneSpanRowPanel(span: HighlyOccurringSpanInfo, project: Project, moreData: HashMap<String, Any>): JPanel {
    val resultPanel = createDefaultBoxLayoutYAxisPanel()
    resultPanel.add(getMainDescriptionPanel(span, project, moreData))
    resultPanel.add(getRowPanel(span))
    return resultPanel
}

private fun getButtonToJaeger(project: Project, insight: EPNPlusSpansInsight): JButton? {
    val spanName = insight.endpointSpanName()
    val sampleTraceId = insight.spans.first().traceId
    val traceSample = TraceSample(spanName, sampleTraceId)
    return buildButtonToJaeger(project, "Trace", spanName, listOf(traceSample))
}
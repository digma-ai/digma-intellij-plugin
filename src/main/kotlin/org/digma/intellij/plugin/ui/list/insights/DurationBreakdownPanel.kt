package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI.Borders.empty
import org.apache.commons.lang3.StringUtils
import org.digma.intellij.plugin.editor.getCurrentPageNumberForInsight
import org.digma.intellij.plugin.editor.updateListOfEntriesToDisplay
import org.digma.intellij.plugin.model.rest.insights.SpanDurationBreakdown
import org.digma.intellij.plugin.model.rest.insights.SpanDurationBreakdownInsight
import org.digma.intellij.plugin.navigation.codeless.showInsightsForSpan
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.boldFonts
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants


private const val P_50: Float = 0.5F
private const val RECORDS_PER_PAGE_DURATION_BREAKDOWN = 3

fun spanDurationBreakdownPanel(
        project: Project,
        insight: SpanDurationBreakdownInsight
): JPanel {

    val uniqueInsightId = insight.codeObjectId + insight.type
    val lastPageNum: Int
    var resultBreakdownPanel: DigmaResettablePanel? = null
    val paginationPanel = JPanel()
    val durationBreakdownEntriesToDisplay = ArrayList<SpanDurationBreakdown>()

    val validBreakdownEntries = insight.breakdownEntries
            .filter { entry -> entry.percentiles.any { breakdown -> breakdown.percentile.equals(P_50) } }
            .sortedWith(compareByDescending { getValueOfPercentile(it, P_50) })

    //calculate how many pages there are
    lastPageNum = validBreakdownEntries.size / RECORDS_PER_PAGE_DURATION_BREAKDOWN + if (validBreakdownEntries.size % RECORDS_PER_PAGE_DURATION_BREAKDOWN != 0) 1 else 0

    resultBreakdownPanel = object : DigmaResettablePanel() {
        override fun reset() {
            rebuildDurationBreakdownRowPanel(
                    resultBreakdownPanel!!,
                    durationBreakdownEntriesToDisplay,
                    project
            )
            rebuildPaginationPanel(paginationPanel, lastPageNum,
                    validBreakdownEntries, resultBreakdownPanel, durationBreakdownEntriesToDisplay, uniqueInsightId, RECORDS_PER_PAGE_DURATION_BREAKDOWN, project, insight.type)
        }
    }

    updateListOfEntriesToDisplay(validBreakdownEntries, durationBreakdownEntriesToDisplay, getCurrentPageNumberForInsight(uniqueInsightId, lastPageNum), RECORDS_PER_PAGE_DURATION_BREAKDOWN, project)
    buildDurationBreakdownRowPanel(resultBreakdownPanel, durationBreakdownEntriesToDisplay, project)

    return createInsightPanel(
            project = project,
            insight = insight,
            title = "Duration Breakdown",
            description = "",
            iconsList = listOf(Laf.Icons.Insight.DURATION),
            bodyPanel = resultBreakdownPanel,
            buttons = null,
            paginationComponent = buildPaginationRowPanel(lastPageNum, paginationPanel,
                    validBreakdownEntries, resultBreakdownPanel, durationBreakdownEntriesToDisplay, uniqueInsightId, RECORDS_PER_PAGE_DURATION_BREAKDOWN, project, insight.type),
    )
}

private fun buildDurationBreakdownRowPanel(
        durationBreakdownPanel: DigmaResettablePanel,
        durationBreakdownEntriesToDisplay: List<SpanDurationBreakdown>,
        project: Project
) {
    durationBreakdownPanel.layout = BoxLayout(durationBreakdownPanel, BoxLayout.Y_AXIS)
    durationBreakdownPanel.isOpaque = false

    durationBreakdownEntriesToDisplay.forEach { durationBreakdown: SpanDurationBreakdown ->
        durationBreakdownPanel.add(durationBreakdownRowPanel(durationBreakdown, project))
    }
}

private fun rebuildDurationBreakdownRowPanel(
        durationBreakdownPanel: DigmaResettablePanel,
        durationBreakdownEntriesToDisplay: List<SpanDurationBreakdown>,
        project: Project
) {
    durationBreakdownPanel.removeAll()
    buildDurationBreakdownRowPanel(durationBreakdownPanel, durationBreakdownEntriesToDisplay, project)
}

private fun durationBreakdownRowPanel(
        durationBreakdown: SpanDurationBreakdown,
        project: Project
): JPanel {
    val durationBreakdownPanel = getDurationBreakdownPanel()
    val telescopeIconLabel = getTelescopeIconLabel()
    val spanDisplayNameLabel = getSpanDisplayNameLabel(durationBreakdown, project)
    val breakdownDurationLabelPanel = getBreakdownDurationLabel(durationBreakdown)

    durationBreakdownPanel.add(telescopeIconLabel, BorderLayout.WEST)
    durationBreakdownPanel.add(spanDisplayNameLabel, BorderLayout.CENTER)
    durationBreakdownPanel.add(breakdownDurationLabelPanel, BorderLayout.EAST)

    return durationBreakdownPanel
}

private fun getDurationBreakdownPanel(): JPanel {
    val durationBreakdownPanel = JBPanel<JBPanel<*>>()
    durationBreakdownPanel.layout = BorderLayout(5, 0)
    durationBreakdownPanel.border = empty()
    durationBreakdownPanel.isOpaque = false
    return durationBreakdownPanel
}

private fun getTelescopeIconLabel(): JLabel {
    val iconLabel = JLabel(Laf.Icons.Insight.TELESCOPE, SwingConstants.LEFT)
    iconLabel.horizontalAlignment = SwingConstants.LEFT
    iconLabel.verticalAlignment = SwingConstants.TOP
    iconLabel.isOpaque = false
    return iconLabel
}

private fun getSpanDisplayNameLabel(
        durationBreakdown: SpanDurationBreakdown,
        project: Project
): JComponent {
    val spanId = durationBreakdown.spanCodeObjectId
    val trimmedDisplayName = StringUtils.normalizeSpace(durationBreakdown.spanDisplayName)

    val messageLabel = ActionLink(trimmedDisplayName) {
        showInsightsForSpan(project, spanId)
    }
    messageLabel.toolTipText = asHtml(trimmedDisplayName)
    messageLabel.border = empty(0, 5, 5, 0)
    messageLabel.isOpaque = false

    messageLabel.minimumSize = messageLabel.preferredSize

    return messageLabel
}

private fun getBreakdownDurationLabel(
        durationBreakdown: SpanDurationBreakdown
): JComponent {
    val pLabelText = getDisplayValueOfPercentile(durationBreakdown, P_50)
    val pLabel = JLabel(pLabelText)
    pLabel.toolTipText = getTooltipForDurationLabel(durationBreakdown)
    boldFonts(pLabel)

    return pLabel
}

private fun getDisplayValueOfPercentile(breakdownEntry: SpanDurationBreakdown, requestedPercentile: Float): String {
    for (pctl in breakdownEntry.percentiles) {
        if (pctl.percentile.equals(requestedPercentile)) {
            return "${pctl.duration.value} ${pctl.duration.unit}"
        }
    }
    return ""
}

private fun getValueOfPercentile(breakdownEntry: SpanDurationBreakdown, requestedPercentile: Float): Long? {
    for (pctl in breakdownEntry.percentiles) {
        if (pctl.percentile.equals(requestedPercentile)) {
            return pctl.duration.raw
        }
    }
    return null
}

private fun getTooltipForDurationLabel(breakdownEntry: SpanDurationBreakdown): String {
    val sortedPercentiles = breakdownEntry.percentiles.sortedBy { it.percentile }
    var tooltip = "Percentage of time spent in span:".plus("<br>")
    for (p in sortedPercentiles) {
        tooltip += "P${(p.percentile * 100).toInt()}: ${p.duration.value} ${p.duration.unit}".plus("<br>")
    }
    return asHtml(tooltip)
}
